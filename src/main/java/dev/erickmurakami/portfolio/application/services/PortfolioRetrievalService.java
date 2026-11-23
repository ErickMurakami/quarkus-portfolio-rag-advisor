package dev.erickmurakami.portfolio.application.services;

import java.util.List;

import dev.erickmurakami.portfolio.application.dto.RetrievedChunk;
import dev.erickmurakami.portfolio.infrastructure.ai.QueryEmbedding;
import dev.erickmurakami.portfolio.infrastructure.repository.DocumentChunkRepository;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PortfolioRetrievalService {

    private static final int TOP_K = 8;

    @Inject
    @QueryEmbedding
    EmbeddingModel model;

    @Inject
    DocumentChunkRepository repository;

    public List<RetrievedChunk> retrieve(Long devId, String question) {
        return search(devId, question, false);
    }

    public List<RetrievedChunk> retrievePublic(Long devId, String question) {
        return search(devId, question, true);
    }

    private List<RetrievedChunk> search(Long devId, String question, boolean publicOnly) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("Question cannot be blank");
        }

        float[] vector = model.embed(TextSegment.from(question.trim())).content().vector();

        return repository.searchSimilar(devId, vector, TOP_K, publicOnly);
    }
}
