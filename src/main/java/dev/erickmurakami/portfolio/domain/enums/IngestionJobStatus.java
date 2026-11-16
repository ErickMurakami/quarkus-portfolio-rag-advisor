package dev.erickmurakami.portfolio.domain.enums;

public enum IngestionJobStatus {
    PENDING,
    RUNNING,
    WAITING_RATE_LIMIT,
    COMPLETED,
    FAILED
}
