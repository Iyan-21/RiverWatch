-- RiverWatch Cebu: Increment 1 schema (accounts, sessions, audit). Names follow SRS Section 5.1.
CREATE TABLE barangay (
    barangay_id        BIGSERIAL PRIMARY KEY,
    name               VARCHAR(120) NOT NULL,
    city_municipality  VARCHAR(120) NOT NULL,
    latitude           NUMERIC(9,6) NOT NULL,
    longitude          NUMERIC(9,6) NOT NULL,
    created_at         TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_barangay UNIQUE (name, city_municipality)
);

CREATE TABLE user_account (
    user_id             BIGSERIAL PRIMARY KEY,
    barangay_id         BIGINT REFERENCES barangay (barangay_id),
    full_name           VARCHAR(120) NOT NULL,
    email               VARCHAR(160) NOT NULL,
    password_hash       VARCHAR(100) NOT NULL,
    phone_number        VARCHAR(20),
    role                VARCHAR(20) NOT NULL,
    account_status      VARCHAR(20) NOT NULL,
    failed_login_count  INT NOT NULL DEFAULT 0,
    locked_until        TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_role CHECK (role IN ('RESIDENT', 'OFFICER', 'ADMIN')),
    CONSTRAINT chk_status CHECK (account_status IN ('PENDING', 'ACTIVE', 'LOCKED', 'REJECTED')),
    CONSTRAINT chk_barangay_required CHECK (role = 'ADMIN' OR barangay_id IS NOT NULL)
);
CREATE UNIQUE INDEX uq_user_email ON user_account (lower(email));

CREATE TABLE user_session (
    session_id          UUID PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES user_account (user_id) ON DELETE CASCADE,
    refresh_token_hash  VARCHAR(64) NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    expires_at          TIMESTAMP NOT NULL,
    revoked_at          TIMESTAMP
);
CREATE INDEX ix_session_user ON user_session (user_id);
CREATE UNIQUE INDEX uq_session_refresh ON user_session (refresh_token_hash);

CREATE TABLE audit_log (
    log_id      BIGSERIAL PRIMARY KEY,
    user_id     BIGINT REFERENCES user_account (user_id) ON DELETE SET NULL,
    action      VARCHAR(60) NOT NULL,
    details     VARCHAR(500),
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

-- Reference data: candidate pilot barangays along the Butuanon River area (Mandaue City).
-- Coordinates are approximate and are to be confirmed with the pilot barangays (SRS Appendix F).
INSERT INTO barangay (name, city_municipality, latitude, longitude) VALUES
    ('Subangdaku',  'Mandaue City', 10.3350, 123.9350),
    ('Tipolo',      'Mandaue City', 10.3420, 123.9460),
    ('Umapad',      'Mandaue City', 10.3380, 123.9560),
    ('Paknaan',     'Mandaue City', 10.3220, 123.9400),
    ('Guizo',       'Mandaue City', 10.3480, 123.9380);
