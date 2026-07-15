CREATE TABLE facilities (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT       NOT NULL REFERENCES tenants (id),
    name        VARCHAR(255) NOT NULL,
    capacity    INT          NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_facilities_tenant_id ON facilities (tenant_id);

CREATE TABLE class_sessions (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT       NOT NULL REFERENCES tenants (id),
    facility_id   BIGINT       NOT NULL REFERENCES facilities (id),
    trainer_id    BIGINT       NOT NULL REFERENCES users (id),
    title         VARCHAR(255) NOT NULL,
    description   TEXT,
    start_time    TIMESTAMPTZ  NOT NULL,
    end_time      TIMESTAMPTZ  NOT NULL,
    max_capacity  INT          NOT NULL,
    version       BIGINT       NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_class_sessions_time CHECK (end_time > start_time)
);

CREATE INDEX idx_class_sessions_trainer_time ON class_sessions (trainer_id, start_time, end_time);
CREATE INDEX idx_class_sessions_facility_time ON class_sessions (facility_id, start_time, end_time);
CREATE INDEX idx_class_sessions_tenant_id ON class_sessions (tenant_id);

CREATE TYPE booking_status AS ENUM ('CONFIRMED', 'CANCELLED', 'LATE_CANCELED', 'ATTENDED', 'NO_SHOW');

CREATE TABLE bookings (
    id               BIGSERIAL PRIMARY KEY,
    tenant_id        BIGINT          NOT NULL REFERENCES tenants (id),
    class_session_id BIGINT          NOT NULL REFERENCES class_sessions (id),
    client_id        BIGINT          NOT NULL REFERENCES users (id),
    status           booking_status  NOT NULL DEFAULT 'CONFIRMED',
    booked_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    cancelled_at     TIMESTAMPTZ,
    created_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_bookings_session_client UNIQUE (class_session_id, client_id)
);

CREATE INDEX idx_bookings_tenant_id ON bookings (tenant_id);
CREATE INDEX idx_bookings_session_status ON bookings (class_session_id, status);
