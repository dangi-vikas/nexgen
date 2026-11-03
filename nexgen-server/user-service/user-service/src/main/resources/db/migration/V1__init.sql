CREATE TABLE users (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    username NVARCHAR(50) NOT NULL,
    email NVARCHAR(100) NOT NULL,
    password NVARCHAR(255) NOT NULL,
    full_name NVARCHAR(100),
    role NVARCHAR(20) NOT NULL,
    enabled BIT NOT NULL,
    created_at DATETIME2
);

CREATE UNIQUE INDEX idx_users_username ON users(username);
CREATE UNIQUE INDEX idx_users_email ON users(email);
ALTER TABLE users ADD CONSTRAINT chk_users_role CHECK (role IN ('ROLE_USER','ROLE_ADMIN','ROLE_VENDOR'));
ALTER TABLE users ADD CONSTRAINT chk_users_enabled CHECK (enabled IN (0,1));
