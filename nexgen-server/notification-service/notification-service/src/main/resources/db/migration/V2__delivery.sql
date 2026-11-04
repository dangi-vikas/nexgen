-- The notification row becomes a delivery outbox: the consumer writes PENDING and
-- the scheduled sender moves it to SENT (or FAILED after the configured attempts).
ALTER TABLE notifications ADD recipient NVARCHAR(255);
ALTER TABLE notifications ADD status NVARCHAR(20) NOT NULL DEFAULT 'PENDING';
ALTER TABLE notifications ADD attempts INT NOT NULL DEFAULT 0;
ALTER TABLE notifications ADD sent_at DATETIME2;
ALTER TABLE notifications ADD provider_message_id NVARCHAR(200);
ALTER TABLE notifications ADD last_error NVARCHAR(500);

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
