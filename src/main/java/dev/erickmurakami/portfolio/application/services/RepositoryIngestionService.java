package dev.erickmurakami.portfolio.application.services;

import java.util.List;

import org.jboss.logging.Logger;

import dev.erickmurakami.portfolio.application.dto.IngestionResult;
import dev.erickmurakami.portfolio.application.dto.RepositoryInfo;
import dev.erickmurakami.portfolio.domain.models.DeveloperProfile;
import dev.erickmurakami.portfolio.domain.models.DocumentChunk;
import dev.erickmurakami.portfolio.infrastructure.github.ReadmeContent;
import dev.erickmurakami.portfolio.infrastructure.repository.DeveloperProfileRepository;
import dev.erickmurakami.portfolio.infrastructure.repository.DocumentChunkRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.exception.RateLimitException;
import dev.langchain4j.model.embedding.EmbeddingModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

@ApplicationScoped
public class RepositoryIngestionService {

    private static final Logger LOG = Logger.getLogger(RepositoryIngestionService.class);

    @Inject
    GithubService githubService;

    @Inject
    DocumentChunkingService documentChunkingService;

    @Inject
    EmbeddingModel model;

    @Inject
    DocumentChunkRepository repository;

    @Inject
    DeveloperProfileRepository developerProfileRepository;

    public int ingestPublicRepository(Long devId, String owner, String repositoryName) {
        ReadmeContent readme = githubService.findPublicReadme(owner, repositoryName);
        return ingestReadme(devId, readme);
    }

    public IngestionResult ingestDeveloperRepositories(Long devId, boolean includePrivate) {

        DeveloperProfile profile = developerProfileRepository.findByIdOptional(devId)
                .orElseThrow(() -> new IllegalArgumentException("Developer not found: " + devId));

        String username = profile.getGithubUsername();

        if (includePrivate) {
            githubService.validateAuthenticatedUser(username);
        }

        List<RepositoryInfo> repos = includePrivate ? githubService.findOwnRepositories()
                : githubService.findPublicRepositories(username);

        int processed = 0;
        int skipped = 0;
        int failed = 0;
        int totalChunks = 0;

        for (RepositoryInfo repo : repos) {
            try {
                int chunks = ingestRepository(devId, username, repo, includePrivate);

                if (chunks == 0) {
                    skipped++;
                    continue;
                }

                processed++;
                totalChunks += chunks;
            } catch (WebApplicationException ex) {
                if (ex.getResponse() != null && ex.getResponse().getStatus() == 404) {
                    skipped++;
                    continue;
                }
                LOG.errorf(ex, "Failed to access repository: %s", repo.name());
                throw ex;
            } catch (RateLimitException ex) {
                LOG.warnf("Voyage AI rate limit reached while indexing: %s", repo.name());
                throw ex;
            } catch (RuntimeException ex) {
                LOG.errorf(ex, "Failed to ingest repository: %s", repo.name());
                failed++;
            }
        }
        return new IngestionResult(repos.size(), processed, skipped, failed, totalChunks);
    }

    private int ingestRepository(Long devId, String username, RepositoryInfo repo, boolean includePrivate) {
        ReadmeContent readme;

        if (includePrivate) {
            readme = githubService.findAuthenticatedReadme(username, repo.name());
        } else {
            readme = githubService.findPublicReadme(username, repo.name());
        }

        return ingestReadme(devId, readme);
    }

    private int ingestReadme(Long devId, ReadmeContent readme) {

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
