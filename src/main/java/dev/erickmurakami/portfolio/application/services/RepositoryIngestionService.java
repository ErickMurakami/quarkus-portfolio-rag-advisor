package dev.erickmurakami.portfolio.application.services;

import java.util.List;

import dev.erickmurakami.portfolio.domain.models.DocumentChunk;
import dev.erickmurakami.portfolio.infrastructure.github.ReadmeContent;
import dev.erickmurakami.portfolio.infrastructure.repository.DocumentChunkRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
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

        List<TextSegment> segments = chunks.stream().map(c -> TextSegment.from(c.getContent())).toList();

        List<Embedding> embeddings = model.embedAll(segments).content();

        if (embeddings.size() != chunks.size()) {
            throw new IllegalStateException("Embedding count mismatch");
        }

        for (int i = 0; i < chunks.size(); i++) {
            chunks.get(i).setEmbedding(embeddings.get(i).vector());
        }

        repository.replaceDocument(devId, readme.sourceReference(), chunks);

        return chunks.size();
    }
}
