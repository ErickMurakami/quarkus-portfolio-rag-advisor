package dev.erickmurakami.portfolio.application.services;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.erickmurakami.portfolio.application.dto.IngestionJobResponse;
import dev.erickmurakami.portfolio.domain.enums.IngestionJobStatus;
import dev.erickmurakami.portfolio.domain.models.IngestionJob;
import dev.erickmurakami.portfolio.infrastructure.repository.DeveloperProfileRepository;
import dev.erickmurakami.portfolio.infrastructure.repository.IngestionJobRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class IngestionJobService {

    @Inject
    IngestionJobRepository repository;

    @Inject
    DeveloperProfileRepository devProfileRepository;

    @Transactional
    public IngestionJobResponse create(Long devId, boolean includePrivate) {
        if (devId == null || devProfileRepository.findByIdOptional(devId).isEmpty()) {
            throw new NotFoundException("Developer not found");
        }

        long activeJobs = repository.count("devId = ?1 and status in ?2", devId,
                List.of(IngestionJobStatus.PENDING, IngestionJobStatus.RUNNING, IngestionJobStatus.WAITING_RATE_LIMIT));

        if (activeJobs > 0) {
            throw new ClientErrorException("Developer already has an active ingestion job", 409);
        }

        IngestionJob job = new IngestionJob(devId, includePrivate);
        repository.persist(job);
        repository.flush();

        return IngestionJobResponse.from(job);
    }

    public IngestionJobResponse findById(UUID id) {
        IngestionJob job = repository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Ingestion Job not found"));
        return IngestionJobResponse.from(job);
    }

    private IngestionJob findByIdInternal(UUID id) {
        return repository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Ingestion Job not found"));
    }

    @Transactional
    public void start(UUID jobId, int totalRepos, String firstRepo) {
        IngestionJob job = findByIdInternal(jobId);
        if (job.getStatus() != IngestionJobStatus.PENDING) {
            throw new IllegalStateException("Job must be pending");
        }
        job.setTotalRepos(totalRepos);
        job.setCurrentRepository(firstRepo);
        job.setUpdatedAt(Instant.now());

        if (firstRepo == null) {
            job.setStatus(IngestionJobStatus.COMPLETED);
            job.setCompletedAt(Instant.now());
        } else {
            job.setStatus(IngestionJobStatus.RUNNING);
        }
    }

    @Transactional
    public void resume(UUID id) {
        IngestionJob job = findByIdInternal(id);

        if (job.getStatus() != IngestionJobStatus.WAITING_RATE_LIMIT) {
            throw new IllegalStateException("Job is not running");
        }

        job.setStatus(IngestionJobStatus.RUNNING);
        job.setNextAttemptAt(null);
        job.setUpdatedAt(Instant.now());
    }

    @Transactional
    public void rateLimited(UUID id, long delaySeconds) {
        IngestionJob job = findByIdInternal(id);

        if (job.getStatus() != IngestionJobStatus.RUNNING) {
            throw new IllegalStateException("Job is not running");
        }

        job.setStatus(IngestionJobStatus.WAITING_RATE_LIMIT);
        job.setNextAttemptAt(Instant.now().plusSeconds(Math.max(1, delaySeconds)));
        job.setUpdatedAt(Instant.now());
    }

    @Transactional
    public void advance(UUID id, String nextRepo, int chunks, boolean skipped) {
        IngestionJob job = findByIdInternal(id);

        if (job.getStatus() != IngestionJobStatus.RUNNING) {
            throw new IllegalStateException("Job is not running");
        }

        if (skipped) {
            job.setSkippedRepos(job.getSkippedRepos() + 1);
        } else {
            job.setProcessedRepos(job.getProcessedRepos() + 1);
            job.setTotalChunks(job.getTotalChunks() + chunks);
        }
    }

    public Optional<IngestionJob> findRunnable() {
        return repository.find("status in ?1 and (nextAttemptAt is null or nextAttemptAt <= ?2) order by createdAt",
                List.of(IngestionJobStatus.PENDING, IngestionJobStatus.RUNNING, IngestionJobStatus.WAITING_RATE_LIMIT),
                Instant.now()).firstResultOptional();
    }
}
