-- Order events carry every line item, which easily exceeds the original 4000
-- character column. A truncated payload would be an unreadable event.
ALTER TABLE outbox_events ALTER COLUMN payload NVARCHAR(MAX);
