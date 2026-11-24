package dev.erickmurakami.portfolio.application.services;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import dev.erickmurakami.portfolio.domain.enums.AccessScope;
import dev.erickmurakami.portfolio.domain.enums.DocumentSource;
import dev.erickmurakami.portfolio.domain.enums.IngestionJobStatus;
import dev.erickmurakami.portfolio.domain.models.DeveloperProfile;
import dev.erickmurakami.portfolio.domain.models.DocumentChunk;
import dev.erickmurakami.portfolio.domain.models.IngestionJob;
import dev.erickmurakami.portfolio.infrastructure.github.ReadmeContent;
import dev.erickmurakami.portfolio.infrastructure.repository.DeveloperProfileRepository;
import dev.erickmurakami.portfolio.infrastructure.repository.DocumentChunkRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;

class PublicIngestionTest {
    @Test
    void shouldKeepPublicFlagDuringDiscoveryAndRepositoryProcessing() {
        var scheduler = new IngestionJobScheduler();
        scheduler.jobs = mock(IngestionJobService.class);
        scheduler.dev = mock(DeveloperProfileRepository.class);
        scheduler.github = mock(GithubService.class);
        scheduler.ingestion = mock(RepositoryIngestionService.class);
        var job = new IngestionJob(1L, false);
        when(scheduler.dev.findByIdOptional(1L)).thenReturn(Optional.of(new DeveloperProfile("Synthetic", "synthetic")));
        when(scheduler.github.findRepoNames("synthetic", false)).thenReturn(List.of("public-demo"));
        scheduler.process(job);
        verify(scheduler.jobs).start(job.getId(), 1, "public-demo");
        job.setStatus(IngestionJobStatus.RUNNING); job.setCurrentRepository("public-demo");
        when(scheduler.ingestion.ingestRepository(1L, "synthetic", "public-demo", false)).thenReturn(2);
        scheduler.process(job);
        verify(scheduler.ingestion).ingestRepository(1L, "synthetic", "public-demo", false);
        verify(scheduler.jobs).advance(job.getId(), null, 2, false);
        verify(scheduler.github, times(2)).findRepoNames("synthetic", false);
        verifyNoMoreInteractions(scheduler.github);
    }

    @Test
    void shouldReadPublicEvidenceWithoutAuthenticatedReadmeOrToken() {
        var service = new RepositoryIngestionService();
        service.githubService = mock(GithubService.class);
        service.documentChunkingService = mock(DocumentChunkingService.class);
        service.model = mock(EmbeddingModel.class);
        service.repository = mock(DocumentChunkRepository.class);
        var readme = new ReadmeContent("demo", "Synthetic public text", AccessScope.PUBLIC, "https://github.com/synthetic/demo");
        var chunks = List.of(new DocumentChunk(1L, readme.content(), DocumentSource.GITHUB, AccessScope.PUBLIC, readme.sourceReference()));
        when(service.githubService.findPublicReadme("synthetic", "demo")).thenReturn(readme);
        when(service.documentChunkingService.chunkReadme(1L, readme)).thenReturn(chunks);
        when(service.model.embedAll(anyList())).thenReturn(Response.from(List.of(Embedding.from(new float[1024]))));
        assertEquals(1, service.ingestRepository(1L, "synthetic", "demo", false));
        verify(service.githubService).findPublicReadme("synthetic", "demo");
        verifyNoMoreInteractions(service.githubService);
        verify(service.repository).replaceDocument(1L, readme.sourceReference(), chunks);
    }
}
