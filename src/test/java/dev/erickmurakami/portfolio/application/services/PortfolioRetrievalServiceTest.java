package dev.erickmurakami.portfolio.application.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.erickmurakami.portfolio.application.dto.RetrievedChunk;
import dev.erickmurakami.portfolio.domain.enums.AccessScope;
import dev.erickmurakami.portfolio.domain.enums.RetrievalMode;
import dev.erickmurakami.portfolio.infrastructure.repository.DocumentChunkRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;

@ExtendWith(MockitoExtension.class)
class PortfolioRetrievalServiceTest {
    @Mock EmbeddingModel model;
    @Mock DocumentChunkRepository repository;
    @InjectMocks PortfolioRetrievalService service;

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void shouldRejectBlankQuestionsBeforeEmbedding(String question) {
        assertEquals("Question cannot be blank", assertThrows(IllegalArgumentException.class,
                () -> service.retrieve(1L, question)).getMessage());
        assertThrows(IllegalArgumentException.class, () -> service.retrievePublic(1L, question));
        assertThrows(IllegalArgumentException.class,
                () -> service.retrievePublic(1L, question, RetrievalMode.OVERVIEW));
        verifyNoInteractions(model, repository);
    }

    @Test
    void shouldKeepInternalRetrievalFocusedAndIncludePrivateEvidence() {
        assertSearch(false, RetrievalMode.FOCUSED, () -> service.retrieve(7L, "  Java?  "));
    }

    @Test
    void shouldDefaultPublicRetrievalToFocused() {
        assertSearch(true, RetrievalMode.FOCUSED, () -> service.retrievePublic(7L, "  Java?  "));
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(RetrievalMode.class)
    void shouldForwardPublicMode(RetrievalMode mode) {
        assertSearch(true, mode, () -> service.retrievePublic(7L, "  Java?  ", mode));
    }

    private void assertSearch(boolean publicOnly, RetrievalMode mode,
            java.util.function.Supplier<List<RetrievedChunk>> search) {
        float[] vector = new float[1024];
        vector[0] = 1;
        when(model.embed(TextSegment.from("Java?"))).thenReturn(Response.from(Embedding.from(vector)));
        var chunks = List.of(new RetrievedChunk(1L, "Synthetic evidence", "synthetic-source",
                publicOnly ? AccessScope.PUBLIC : AccessScope.PRIVATE, 0.9));
        when(repository.searchSimilar(7L, vector, 8, publicOnly, mode)).thenReturn(chunks);
        assertSame(chunks, search.get());
        verify(model).embed(TextSegment.from("Java?"));
        verify(repository).searchSimilar(7L, vector, 8, publicOnly, mode);
        verifyNoMoreInteractions(model, repository);
    }
}
