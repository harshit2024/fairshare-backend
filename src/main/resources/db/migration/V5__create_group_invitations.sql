
CREATE TABLE group_invitations (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL
        REFERENCES expense_groups(id) ON DELETE CASCADE,
    invited_by UUID NOT NULL REFERENCES users(id),
    invitee_email VARCHAR(255) NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (
            status IN ('PENDING', 'ACCEPTED', 'EXPIRED', 'REVOKED')
        ),
    expires_at TIMESTAMP NOT NULL,
    accepted_by UUID REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accepted_at TIMESTAMP
);

CREATE INDEX idx_group_invitations_group
    ON group_invitations(group_id);

CREATE INDEX idx_group_invitations_email
    ON group_invitations(invitee_email);

CREATE INDEX idx_group_invitations_status_expiry
    ON group_invitations(status, expires_at);