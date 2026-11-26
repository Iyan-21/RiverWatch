-- Increment 2: single-use, expiring password reset tokens (SRS FR-006, NFR-S06 style: only hashes are stored).
CREATE TABLE password_reset_token (
    token_id    BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES user_account (user_id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    expires_at  TIMESTAMP NOT NULL,
    used_at     TIMESTAMP
);
CREATE UNIQUE INDEX uq_reset_token_hash ON password_reset_token (token_hash);
CREATE INDEX ix_reset_token_user ON password_reset_token (user_id);

-- Supabase exposes the public schema through a REST API. Row level security with no policies blocks that
-- API completely, while the backend (table owner / postgres role) is not affected.
ALTER TABLE barangay              ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_account          ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_session          ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_log             ENABLE ROW LEVEL SECURITY;
ALTER TABLE password_reset_token  ENABLE ROW LEVEL SECURITY;
