CREATE TYPE invoice_status AS ENUM ('UNPAID', 'PAID', 'CANCELLED');

CREATE TABLE client_accounts (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT         NOT NULL REFERENCES tenants (id),
    client_id   BIGINT         NOT NULL REFERENCES users (id),
    balance     NUMERIC(12, 2) NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_client_accounts_client UNIQUE (tenant_id, client_id)
);

CREATE INDEX idx_client_accounts_tenant_id ON client_accounts (tenant_id);

CREATE TABLE invoices (
    id                    BIGSERIAL PRIMARY KEY,
    tenant_id             BIGINT         NOT NULL REFERENCES tenants (id),
    client_id             BIGINT         NOT NULL REFERENCES users (id),
    client_membership_id  BIGINT         REFERENCES client_memberships (id),
    amount                NUMERIC(10, 2) NOT NULL,
    status                invoice_status NOT NULL DEFAULT 'UNPAID',
    due_date              DATE           NOT NULL,
    paid_at               TIMESTAMPTZ,
    created_at            TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_invoices_tenant_id ON invoices (tenant_id);
CREATE INDEX idx_invoices_client_status ON invoices (client_id, status);

CREATE TYPE payment_method AS ENUM ('CASH', 'CARD', 'TRANSFER');

CREATE TABLE transactions (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT         NOT NULL REFERENCES tenants (id),
    invoice_id  BIGINT         NOT NULL REFERENCES invoices (id),
    amount      NUMERIC(10, 2) NOT NULL,
    method      payment_method NOT NULL,
    processed_at TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transactions_tenant_id ON transactions (tenant_id);

CREATE TYPE payroll_status AS ENUM ('PENDING', 'CALCULATED', 'PAID');

CREATE TABLE trainer_payroll (
    id               BIGSERIAL PRIMARY KEY,
    tenant_id        BIGINT         NOT NULL REFERENCES tenants (id),
    trainer_id       BIGINT         NOT NULL REFERENCES users (id),
    class_session_id BIGINT         NOT NULL REFERENCES class_sessions (id),
    base_amount      NUMERIC(10, 2) NOT NULL,
    bonus_amount     NUMERIC(10, 2) NOT NULL DEFAULT 0,
    total_amount     NUMERIC(10, 2) NOT NULL,
    status           payroll_status NOT NULL DEFAULT 'PENDING',
    calculated_at    TIMESTAMPTZ,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_trainer_payroll_tenant_id ON trainer_payroll (tenant_id);
CREATE INDEX idx_trainer_payroll_trainer_status ON trainer_payroll (trainer_id, status);
