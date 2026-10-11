ALTER TABLE users
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
    CHECK (status IN ('ACTIVE', 'LOCKED', 'DISABLED'));

ALTER TABLE users
ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE users
ADD COLUMN failed_login_attempts INTEGER NOT NULL DEFAULT 0
    CHECK (failed_login_attempts >= 0);

ALTER TABLE users
ADD COLUMN locked_until TIMESTAMP;

ALTER TABLE users
ADD COLUMN last_login_at TIMESTAMP;

CREATE UNIQUE INDEX idx_users_email_lower
    ON users (LOWER(email));