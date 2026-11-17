package dev.erickmurakami.portfolio.application.services;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;

import dev.erickmurakami.portfolio.domain.enums.IngestionJobStatus;
import dev.erickmurakami.portfolio.domain.models.IngestionJob;
import dev.erickmurakami.portfolio.infrastructure.repository.DeveloperProfileRepository;
import dev.langchain4j.exception.RateLimitException;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

@ApplicationScoped
public class IngestionJobScheduler {

    private static final Logger LOG = Logger.getLogger(IngestionJobScheduler.class);

    private static final Pattern RETRY_DELAY = Pattern.compile("Please retry in\\s+(\\d+)");

    @Inject
    IngestionJobService jobs;

    @Inject
    DeveloperProfileRepository dev;

    @Inject
    GithubService github;

    @Inject
    RepositoryIngestionService ingestion;

    @Scheduled(every = "5s", concurrentExecution = ConcurrentExecution.SKIP)
    void execute() {
        jobs.findRunnable().ifPresent(this::process);
    }

    void process(IngestionJob job) {
        UUID id = job.getId();

        try {
            var developer = dev.findByIdOptional(job.getDevId()).orElseThrow();

            String username = developer.getGithubUsername();

            List<String> repos = github.findRepoNames(username, job.isIncludePrivate());

            if (job.getStatus() == IngestionJobStatus.PENDING) {
                jobs.start(id, repos.size(), repos.isEmpty() ? null : repos.getFirst());
                return;
            }

            String current = job.getCurrentRepository();
            int index = repos.indexOf(current);

            if (index < 0) {
                throw new IllegalStateException("Repository cursor not found: " + current);
            }

            String next = index + 1 < repos.size() ? repos.get(index + 1) : null;

            if (job.getStatus() == IngestionJobStatus.WAITING_RATE_LIMIT) {
                jobs.resume(id);
            }

            try {
                int chunks = ingestion.ingestRepository(job.getDevId(), username, current, job.isIncludePrivate());
                jobs.advance(id, next, chunks, chunks == 0);
                LOG.infof("Indexed %s: %d chunks", current, chunks);
            } catch (RateLimitException ex) {
                long delay = retryDelay(ex);

                jobs.rateLimited(id, delay);

                LOG.warnf("Gemini 429: %s. Retrying in %ds", current, delay);
            } catch (WebApplicationException ex) {
                if (ex.getResponse() != null && ex.getResponse().getStatus() == 404) {
                    jobs.advance(id, next, 0, true);
                    LOG.infof("Readme not found: %s", current);
                } else {
                    throw ex;
                }
            }
        } catch (Exception ex) {
            LOG.errorf(ex, "Ingestion job %s failed", id);
            jobs.fail(id, ex.getMessage());
        }
    }

    private long retryDelay(RateLimitException ex) {
        String message = ex.getMessage();

        if (message == null) {
            return 70;
        }

        if (message.contains("PerDay") || message.contains("per day")) {
            return 86_400;
        }

        Matcher matcher = RETRY_DELAY.matcher(message);

        if (matcher.find()) {
            return Long.parseLong(matcher.group(1)) + 5;
        }

        return 70;
    }
}
