package dev.erickmurakami.portfolio.domain.models;

import java.time.Instant;
import java.util.UUID;

import dev.erickmurakami.portfolio.domain.enums.IngestionJobStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ingestion_job")
public class IngestionJob {

    @Id
    private UUID id;

    @Column(name = "dev_id", nullable = false)
    private Long devId;

    @Column(name = "include_private", nullable = false)
    private boolean includePrivate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IngestionJobStatus status;

    @Column(name = "total_repos", nullable = false)
    private int totalRepos;

    @Column(name = "processed_repos", nullable = false)
    private int processedRepos;

    @Column(name = "skipped_repos", nullable = false)
    private int skippedRepos;

    @Column(name = "failed_repos", nullable = false)
    private int failedRepos;

    @Column(name = "total_chunks", nullable = false)
    private int totalChunks;

    @Column(name = "current_repository")
    private String currentRepository;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected IngestionJob() {
    }

    public IngestionJob(Long devId, boolean includePrivate) {
        if (devId == null) {
            throw new IllegalArgumentException("Developer ID cannot be null");
        }
        this.id = UUID.randomUUID();
        this.devId = devId;
        this.includePrivate = includePrivate;
        this.status = IngestionJobStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public Long getDevId() {
        return devId;
    }

    public boolean isIncludePrivate() {
        return includePrivate;
    }

    public IngestionJobStatus getStatus() {
        return status;
    }

    public int getTotalRepos() {
        return totalRepos;
    }

    public int getProcessedRepos() {
        return processedRepos;
    }

    public int getSkippedRepos() {
        return skippedRepos;
    }

    public int getFailedRepos() {
        return failedRepos;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public String getCurrentRepository() {
        return currentRepository;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

}
