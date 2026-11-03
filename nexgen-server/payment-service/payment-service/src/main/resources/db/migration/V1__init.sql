CREATE TABLE payments (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_number NVARCHAR(100) NOT NULL,
    user_id NVARCHAR(100),
    amount DECIMAL(19,2) NOT NULL,
    currency NVARCHAR(3) NOT NULL,
    status NVARCHAR(20) NOT NULL,
    provider NVARCHAR(50),
    provider_ref NVARCHAR(100),
    failure_reason NVARCHAR(500),
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    version BIGINT
);

CREATE UNIQUE INDEX uk_payments_order ON payments(order_number);
ALTER TABLE payments ADD CONSTRAINT chk_payments_amount CHECK (amount > 0);
ALTER TABLE payments ADD CONSTRAINT chk_payments_status CHECK (status IN ('PENDING','AUTHORIZED','FAILED','REFUNDED'));

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

CREATE TABLE processed_events (
    event_id NVARCHAR(100) PRIMARY KEY,
    event_type NVARCHAR(100),
    processed_at DATETIME2 NOT NULL
);

CREATE INDEX idx_processed_events_at ON processed_events(processed_at);
