
CREATE TABLE activity_logs (
    id UUID PRIMARY KEY,
    group_id UUID REFERENCES expense_groups(id),
    actor_id UUID REFERENCES users(id) ON DELETE SET NULL,
    entity_type VARCHAR(40) NOT NULL,
    entity_id UUID,
    action VARCHAR(60) NOT NULL,
    details JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_activity_logs_group_date
    ON activity_logs(group_id, created_at DESC);

CREATE INDEX idx_activity_logs_actor
    ON activity_logs(actor_id);

CREATE INDEX idx_activity_logs_entity
    ON activity_logs(entity_type, entity_id);