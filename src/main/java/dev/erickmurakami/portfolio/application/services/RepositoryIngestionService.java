package dev.erickmurakami.portfolio.application.services;

import java.util.List;

import dev.erickmurakami.portfolio.domain.models.DocumentChunk;
import dev.erickmurakami.portfolio.infrastructure.github.ReadmeContent;
import dev.erickmurakami.portfolio.infrastructure.repository.DocumentChunkRepository;
import dev.langchain4j.model.embedding.EmbeddingModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RepositoryIngestionService {

    @Inject
    GithubService githubService;

    @Inject
    DocumentChunkingService documentChunkingService;

    @Inject
    EmbeddingModel model;

    @Inject
    DocumentChunkRepository repository;

    public int ingestPublicRepository(Long devId, String owner, String repositoryName) {
        ReadmeContent readme = githubService.findPublicReadme(owner, repositoryName);

        List<DocumentChunk> chunks = documentChunkingService.chunkReadme(devId, readme);

        if (chunks.isEmpty()) {
            return 0;
        }

        for (DocumentChunk chunk : chunks) {
            float[] vector = model.embed(chunk.getContent()).content().vector();
            chunk.setEmbedding(vector);
        }

        repository.replaceDocument(devId, readme.sourceReference(), chunks);

        return chunks.size();
    }
}
