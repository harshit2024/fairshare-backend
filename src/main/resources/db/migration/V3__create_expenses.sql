
CREATE TABLE expenses (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL REFERENCES expense_groups(id),
    description VARCHAR(255) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    currency CHAR(3) NOT NULL DEFAULT 'INR',
    paid_by UUID NOT NULL REFERENCES users(id),
    created_by UUID NOT NULL REFERENCES users(id),
    category VARCHAR(40) NOT NULL DEFAULT 'OTHER'
        CHECK (
            category IN (
                'FOOD',
                'TRAVEL',
                'RENT',
                'UTILITIES',
                'SHOPPING',
                'ENTERTAINMENT',
                'OTHER'
            )
        ),
    split_type VARCHAR(20) NOT NULL DEFAULT 'EQUAL'
        CHECK (
            split_type IN ('EQUAL', 'EXACT', 'PERCENTAGE', 'SHARES')
        ),
    expense_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE expense_splits (
    id UUID PRIMARY KEY,
    expense_id UUID NOT NULL
        REFERENCES expenses(id) ON DELETE RESTRICT,
    user_id UUID NOT NULL REFERENCES users(id),
    share_amount NUMERIC(12, 2) NOT NULL
        CHECK (share_amount >= 0),
    percentage NUMERIC(7, 4)
        CHECK (percentage >= 0 AND percentage <= 100),
    shares NUMERIC(12, 4)
        CHECK (shares >= 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (expense_id, user_id)
);

CREATE INDEX idx_expenses_group_date
    ON expenses(group_id, expense_date DESC);

CREATE INDEX idx_expenses_paid_by
    ON expenses(paid_by);

CREATE INDEX idx_expenses_created_by
    ON expenses(created_by);

CREATE INDEX idx_expense_splits_user
    ON expense_splits(user_id);

CREATE INDEX idx_expense_splits_expense
    ON expense_splits(expense_id);