ALTER TABLE expense_groups
ADD COLUMN owner_id UUID;

UPDATE expense_groups
SET owner_id = created_by
WHERE owner_id IS NULL;

ALTER TABLE expense_groups
ALTER COLUMN owner_id SET NOT NULL;

ALTER TABLE expense_groups
ADD CONSTRAINT fk_expense_groups_owner
FOREIGN KEY (owner_id)
REFERENCES users(id);

CREATE INDEX idx_expense_groups_owner
ON expense_groups(owner_id);