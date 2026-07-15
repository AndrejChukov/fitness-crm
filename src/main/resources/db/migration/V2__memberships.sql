CREATE TYPE membership_status AS ENUM ('ACTIVE', 'FROZEN', 'DEPLETED', 'EXPIRED');

CREATE TABLE membership_templates (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT         NOT NULL REFERENCES tenants (id),
    name            VARCHAR(255)   NOT NULL,
    description     TEXT,
    price           NUMERIC(10, 2) NOT NULL,
    class_limit     INT,
    duration_days   INT            NOT NULL,
    active          BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_membership_templates_tenant_id ON membership_templates (tenant_id);

CREATE TABLE client_memberships (
    id                BIGSERIAL PRIMARY KEY,
    tenant_id         BIGINT             NOT NULL REFERENCES tenants (id),
    client_id         BIGINT             NOT NULL REFERENCES users (id),
    template_id       BIGINT             NOT NULL REFERENCES membership_templates (id),
    status            membership_status  NOT NULL DEFAULT 'ACTIVE',
    remaining_classes INT,
    freeze_days_used  INT                NOT NULL DEFAULT 0,
    frozen_at         TIMESTAMPTZ,
    start_date        DATE               NOT NULL,
    end_date          DATE               NOT NULL,
    created_at        TIMESTAMPTZ        NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ        NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_client_memberships_tenant_id ON client_memberships (tenant_id);
CREATE INDEX idx_client_memberships_client_status ON client_memberships (client_id, status);
