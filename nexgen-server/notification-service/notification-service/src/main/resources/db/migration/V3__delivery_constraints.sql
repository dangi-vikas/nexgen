-- Index, constraint and address book for the delivery outbox. These depend on the
-- columns added in V2, so they must live in their own migration/batch.
CREATE INDEX idx_notifications_status ON notifications(status, created_at);
ALTER TABLE notifications ADD CONSTRAINT chk_notifications_status
    CHECK (status IN ('PENDING', 'SENT', 'FAILED'));

-- Address book built from user-registration / user-profile-updated, so order
-- notifications have an address to send to without a synchronous user lookup.
CREATE TABLE user_contacts (
    user_id NVARCHAR(100) PRIMARY KEY,
    email NVARCHAR(255) NOT NULL,
    updated_at DATETIME2 NOT NULL
);
