-- The notification row becomes a delivery outbox: the consumer writes PENDING and
-- the scheduled sender moves it to SENT (or FAILED after the configured attempts).
--
-- Only plain column additions live here. Objects that reference the new columns
-- (index, CHECK constraint) go in V3: SQL Server compiles a batch as a unit, so a
-- constraint on a column added earlier in the same script fails with
-- "Invalid column name" before any statement runs.
ALTER TABLE notifications ADD recipient NVARCHAR(255);
ALTER TABLE notifications ADD status NVARCHAR(20) NOT NULL DEFAULT 'PENDING';
ALTER TABLE notifications ADD attempts INT NOT NULL DEFAULT 0;
ALTER TABLE notifications ADD sent_at DATETIME2;
ALTER TABLE notifications ADD provider_message_id NVARCHAR(200);
ALTER TABLE notifications ADD last_error NVARCHAR(500);
