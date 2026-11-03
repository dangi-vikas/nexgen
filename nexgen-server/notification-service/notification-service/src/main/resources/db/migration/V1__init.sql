CREATE TABLE notifications (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    event_id NVARCHAR(100) NOT NULL,
    user_id NVARCHAR(100),
    order_number NVARCHAR(100),
    type NVARCHAR(50) NOT NULL,
    subject NVARCHAR(200),
    message NVARCHAR(1000),
    created_at DATETIME2 NOT NULL
);

CREATE UNIQUE INDEX uk_notifications_event_id ON notifications(event_id);
CREATE INDEX idx_notifications_user ON notifications(user_id, created_at);
