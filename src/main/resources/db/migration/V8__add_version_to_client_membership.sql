ALTER TABLE client_memberships
    ADD COLUMN version BIGINT NOT NULL DEFAULT 1;