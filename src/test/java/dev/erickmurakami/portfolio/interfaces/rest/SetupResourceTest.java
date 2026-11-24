package dev.erickmurakami.portfolio.interfaces.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import dev.erickmurakami.portfolio.application.dto.DeveloperProfileRequest;
import dev.erickmurakami.portfolio.application.dto.IngestionJobResponse;
import dev.erickmurakami.portfolio.application.services.IngestionJobService;
import dev.erickmurakami.portfolio.application.services.ResumeImportService;
import dev.erickmurakami.portfolio.domain.enums.IngestionJobStatus;
import io.quarkus.runtime.LaunchMode;
import jakarta.ws.rs.BadRequestException;

class SetupResourceTest {
    @TempDir Path directory;

    @Test
    void shouldCreateOnlyPublicJobAndReturnRealStatusAndLocation() {
        var service = mock(IngestionJobService.class);
        var resource = new IngestionJobResource(); resource.service = service;
        UUID id = UUID.randomUUID();
        var job = new IngestionJobResponse(id, 7L, IngestionJobStatus.PENDING, 0, 0, 0, 0, 0, null, null);
        when(service.create(7L, false)).thenReturn(job);
        var response = resource.createPublic(7L);
        assertEquals(202, response.getStatus());
        assertSame(job, response.getEntity());
        assertEquals(URI.create("/api/v1/ingestion-jobs/" + id), response.getLocation());
        when(service.findById(id)).thenReturn(job);
        assertSame(job, resource.findById(id));
        verify(service).create(7L, false);
        verify(service, never()).create(anyLong(), eq(true));
    }

    @Test
    void shouldExtractForReviewWithoutSavingAndDeleteTemporaryPdf() throws Exception {
        var resource = new ResumeResource(); resource.service = mock(ResumeImportService.class);
        Path path = directory.resolve("resume.pdf"); Files.writeString(path, "%PDF-synthetic test");
        FileUpload upload = upload(path, Files.size(path));
        var draft = new DeveloperProfileRequest("Synthetic", "synthetic", List.of());
        when(resource.service.importResume(path, "synthetic")).thenReturn(draft);
        assertSame(draft, resource.importResume(upload, "synthetic"));
        assertFalse(Files.exists(path));
        verify(resource.service).importResume(path, "synthetic");
        verifyNoMoreInteractions(resource.service);
    }

    @Test
    void shouldRejectMissingInvalidAndOversizedUploadsBeforeAiAndCleanUp() throws Exception {
        var resource = new ResumeResource(); resource.service = mock(ResumeImportService.class);
        assertThrows(BadRequestException.class, () -> resource.importResume(null, "synthetic"));
        for (long size : List.of(0L, 4L * 1024 * 1024 + 1, 5L)) {
            Path path = directory.resolve("invalid.pdf"); Files.writeString(path, "INVALID");
            var upload = upload(path, size);
            assertThrows(BadRequestException.class, () -> resource.importResume(upload, "synthetic"));
            assertFalse(Files.exists(path));
        }
        Path path = directory.resolve("missing-username.pdf"); Files.writeString(path, "%PDF-test");
        var upload = upload(path, 9L);
        assertThrows(BadRequestException.class, () -> resource.importResume(upload, " "));
        assertFalse(Files.exists(path));
        verifyNoInteractions(resource.service);
    }

    @Test
    void shouldDeletePdfAndAvoidLeakingModelResponseWhenExtractionFails() throws Exception {
        var resource = new ResumeResource(); resource.service = mock(ResumeImportService.class);
        Path path = directory.resolve("test.pdf"); Files.writeString(path, "%PDF-test");
        when(resource.service.importResume(path, "synthetic")).thenThrow(new RuntimeException("SYNTHETIC PRIVATE TEXT"));
        var upload = upload(path, 9L);
        var exception = assertThrows(BadRequestException.class, () -> resource.importResume(upload, "synthetic"));
        assertFalse(exception.getMessage().contains("SYNTHETIC PRIVATE TEXT"));
        assertFalse(Files.exists(path));
    }

    @Test
    void shouldRequireLocalDevAndSameOriginRatherThanTreatingDeveloperIdAsAuthorization() {
        URI base = URI.create("http://localhost:8000/");
        assertFalse(LocalApiFilter.isLocalRequest(LaunchMode.NORMAL, "127.0.0.1", null, base));
        assertFalse(LocalApiFilter.isLocalRequest(LaunchMode.TEST, "127.0.0.1", null, base));
        assertFalse(LocalApiFilter.isLocalRequest(LaunchMode.DEVELOPMENT, "203.0.113.1", null, base));
        assertFalse(LocalApiFilter.isLocalRequest(LaunchMode.DEVELOPMENT, "127.0.0.1", "https://untrusted.example", base));
        assertTrue(LocalApiFilter.isLocalRequest(LaunchMode.DEVELOPMENT, "127.0.0.1", "http://localhost:8000", base));
        assertTrue(LocalApiFilter.isLocalRequest(LaunchMode.DEVELOPMENT, "::1", null, base));
    }

    private FileUpload upload(Path path, long size) {
        var upload = mock(FileUpload.class);
        when(upload.uploadedFile()).thenReturn(path);
        when(upload.size()).thenReturn(size);
        return upload;
    }
}
