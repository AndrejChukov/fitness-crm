CREATE TABLE tenants (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(100) NOT NULL UNIQUE,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TYPE user_role AS ENUM ('SUPER_ADMIN', 'TENANT_ADMIN', 'TRAINER', 'CLIENT');

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT REFERENCES tenants (id),
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          user_role    NOT NULL,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_users_tenant_email UNIQUE (tenant_id, email)
);

CREATE INDEX idx_users_tenant_id ON users (tenant_id);
CREATE INDEX idx_users_email ON users (email);
