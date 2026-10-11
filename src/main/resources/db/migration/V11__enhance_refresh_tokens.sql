ALTER TABLE refresh_tokens
ADD COLUMN last_used_at TIMESTAMP;

ALTER TABLE refresh_tokens
ADD COLUMN replaced_by UUID;

ALTER TABLE refresh_tokens
ADD CONSTRAINT fk_refresh_tokens_replaced_by
FOREIGN KEY (replaced_by)
REFERENCES refresh_tokens(id);

ALTER TABLE refresh_tokens
ADD COLUMN user_agent TEXT;

ALTER TABLE refresh_tokens
ADD COLUMN ip_address INET;

CREATE INDEX idx_refresh_tokens_active
ON refresh_tokens(user_id, revoked_at, expires_at);