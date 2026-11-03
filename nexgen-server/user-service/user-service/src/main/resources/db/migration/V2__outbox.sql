CREATE TABLE outbox_events (
    id NVARCHAR(36) PRIMARY KEY,
    aggregate_type NVARCHAR(50),
    aggregate_id NVARCHAR(100),
    event_type NVARCHAR(100) NOT NULL,
    event_version NVARCHAR(10),
    occurred_at DATETIME2 NOT NULL,
    producer NVARCHAR(50),
    correlation_id NVARCHAR(100),
    causation_id NVARCHAR(100),
    payload NVARCHAR(MAX),
    status NVARCHAR(20) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    published_at DATETIME2
);

CREATE INDEX idx_outbox_status_time ON outbox_events(status, occurred_at);
ALTER TABLE outbox_events ADD CONSTRAINT chk_outbox_status CHECK (status IN ('PENDING','PUBLISHED','FAILED'));
