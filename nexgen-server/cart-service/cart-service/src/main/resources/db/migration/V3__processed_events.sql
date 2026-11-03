CREATE TABLE processed_events (
    event_id NVARCHAR(100) PRIMARY KEY,
    event_type NVARCHAR(100),
    processed_at DATETIME2 NOT NULL
);
