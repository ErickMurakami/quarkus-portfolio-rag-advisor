package dev.erickmurakami.portfolio.application.dto;

import java.time.Instant;
import java.util.UUID;

import dev.erickmurakami.portfolio.domain.enums.IngestionJobStatus;
import dev.erickmurakami.portfolio.domain.models.IngestionJob;

public record IngestionJobResponse(
        UUID jobId,
        Long devId,
        IngestionJobStatus status,
        int totalRepositories,
        int processedRepositories,
        int skippedRepositories,
        int failedRepositories,
        int totalChunks,
        String currentRepository,
        Instant nextAttemptAt) {
    public static IngestionJobResponse from(IngestionJob j) {
        return new IngestionJobResponse(j.getId(),
                j.getDevId(),
                j.getStatus(),
                j.getTotalRepos(),
                j.getProcessedRepos(),
                j.getSkippedRepos(),
                j.getFailedRepos(),
                j.getTotalChunks(),
                j.getCurrentRepository(),
                j.getNextAttemptAt());
    }
}
