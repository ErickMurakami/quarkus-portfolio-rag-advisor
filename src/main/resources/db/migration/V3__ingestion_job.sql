CREATE TABLE ingestion_job (
    id UUID PRIMARY KEY,
    dev_id BIGINT NOT NULL,
    include_private BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(30) NOT NULL,
    total_repos INTEGER NOT NULL DEFAULT 0,
    processed_repos INTEGER NOT NULL DEFAULT 0,
    skipped_repos INTEGER NOT NULL DEFAULT 0,
    failed_repos INTEGER NOT NULL DEFAULT 0,
    total_chunks INTEGER NOT NULL DEFAULT 0,
    current_repository VARCHAR(255),
    next_attempt_at TIMESTAMPTZ,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,

    CONSTRAINT fk_ingestion_job_dev FOREIGN KEY (dev_id) REFERENCES dev_profile(id) ON DELETE CASCADE,

    CONSTRAINT ck_ingestion_job_status CHECK (status IN(
        'PENDING',
        'RUNNING',
        'WAITING_RATE_LIMIT',
        'COMPLETED',
        'FAILED'
    ))
);

CREATE UNIQUE INDEX ux_ingestion_job_active_dev ON ingestion_job(dev_id)
    WHERE status IN ('PENDING', 'RUNNING', 'WAITING_RATE_LIMIT');

CREATE INDEX idx_ingestion_job_schedule ON ingestion_job(status, next_attempt_at);