ALTER TABLE users ADD COLUMN status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'locked', 'suspended'));
ALTER TABLE users ADD COLUMN last_login_at INTEGER;
ALTER TABLE users ADD COLUMN token_version INTEGER NOT NULL DEFAULT 1;

ALTER TABLE auth_throttles ADD COLUMN email_hint TEXT;

CREATE TABLE IF NOT EXISTS access_logs (
  id TEXT PRIMARY KEY,
  user_id TEXT REFERENCES users(id),
  email TEXT NOT NULL,
  role TEXT,
  event_type TEXT NOT NULL,
  outcome TEXT NOT NULL CHECK (outcome IN ('success', 'failed', 'blocked')),
  ip_address TEXT,
  user_agent TEXT,
  detail TEXT,
  created_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_access_logs_created
  ON access_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_access_logs_outcome_created
  ON access_logs(outcome, created_at DESC);

CREATE TABLE IF NOT EXISTS admin_invites (
  id TEXT PRIMARY KEY,
  code_hash TEXT NOT NULL UNIQUE,
  code_preview TEXT NOT NULL,
  label TEXT NOT NULL,
  station_id TEXT NOT NULL REFERENCES stations(id),
  max_uses INTEGER NOT NULL DEFAULT 1 CHECK (max_uses > 0),
  used_count INTEGER NOT NULL DEFAULT 0 CHECK (used_count >= 0),
  expires_at INTEGER NOT NULL,
  revoked_at INTEGER,
  created_by TEXT REFERENCES users(id),
  created_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_admin_invites_created
  ON admin_invites(created_at DESC);
