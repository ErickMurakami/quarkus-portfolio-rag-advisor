package dev.erickmurakami.portfolio.application.dto;

public record IngestionResult(
        int totalRepositories,
        int processedRepositories,
        int skippedRepositories,
        int failedRepositories,
        int totalChunks) {

}
