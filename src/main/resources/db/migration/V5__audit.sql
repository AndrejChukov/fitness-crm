CREATE TYPE audit_action AS ENUM ('CREATE', 'UPDATE', 'DELETE');

CREATE TABLE audit_logs (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT,
    entity_name     VARCHAR(100) NOT NULL,
    entity_id       BIGINT       NOT NULL,
    action          audit_action NOT NULL,
    changed_by_user BIGINT       REFERENCES users (id),
    old_value       TEXT,
    new_value       TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_tenant_id ON audit_logs (tenant_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_name, entity_id);

CREATE TABLE revinfo (
    rev      BIGSERIAL PRIMARY KEY,
    revtstmp BIGINT NOT NULL
);
