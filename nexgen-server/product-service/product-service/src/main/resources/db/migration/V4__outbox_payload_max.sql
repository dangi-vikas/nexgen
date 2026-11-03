-- Product events are small today, but the column must not silently truncate a
-- payload and turn it into an unreadable event.
ALTER TABLE outbox_events ALTER COLUMN payload NVARCHAR(MAX);
