package dev.erickmurakami.portfolio.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.erickmurakami.portfolio.infrastructure.document.PdfTextExtractor;

@ExtendWith(MockitoExtension.class)
public class ResumeImportServiceTest {

    @Mock
    PdfTextExtractor textExtractor;

    @Mock
    ResumeAiExtractor aiExtractor;

    @Spy
    ObjectMapper mapper = new ObjectMapper();

    @InjectMocks
    ResumeImportService service;

    @Test
    void shouldExtractDeveloperProfile() throws IOException {
        Path pdf = Path.of("resume.pdf");

        when(textExtractor.extract(pdf)).thenReturn("John Doe - Java Developer");

        when(aiExtractor.extractDeveloperProfileFromResume(anyString()))
                .thenReturn("""
                            {
                                "fullName": "John Doe",
                                "skills":[
                                    {
                                        "name": "Java",
                                        "category": "LANGUAGE"
                                    }
                                ]
                            }
                        """);

        var result = service.importResume(pdf, "JohnDoe");

        assertEquals("John Doe", result.fullName());
        assertEquals("JohnDoe", result.githubUsername());
        assertEquals(1, result.skills().size());
        assertEquals("Java", result.skills().getFirst().name());
    }
    @Test
    void shouldRejectEmptyResumeTextBeforeCallingAi() throws IOException {
        Path pdf = Path.of("synthetic-empty.pdf");
        when(textExtractor.extract(pdf)).thenReturn("   ");
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> service.importResume(pdf, "synthetic"));
        org.mockito.Mockito.verifyNoInteractions(aiExtractor);
    }

    @Test
    void shouldRejectExcessiveResumeTextBeforeCallingAi() throws IOException {
        Path pdf = Path.of("synthetic-large.pdf");
        when(textExtractor.extract(pdf)).thenReturn("a".repeat(100_001));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> service.importResume(pdf, "synthetic"));
        org.mockito.Mockito.verifyNoInteractions(aiExtractor);
    }

}
