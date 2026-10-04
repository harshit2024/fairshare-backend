
CREATE TABLE settlements (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL REFERENCES expense_groups(id),
    paid_by UUID NOT NULL REFERENCES users(id),
    paid_to UUID NOT NULL REFERENCES users(id),
    amount NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    currency CHAR(3) NOT NULL DEFAULT 'INR',
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED'
        CHECK (
            status IN ('PENDING', 'COMPLETED', 'CANCELLED')
        ),
    payment_method VARCHAR(30),
    transaction_reference VARCHAR(255),
    notes TEXT,
    settled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (paid_by <> paid_to)
);

CREATE INDEX idx_settlements_group
    ON settlements(group_id);

CREATE INDEX idx_settlements_paid_by
    ON settlements(paid_by);

CREATE INDEX idx_settlements_paid_to
    ON settlements(paid_to);

CREATE INDEX idx_settlements_status
    ON settlements(status);