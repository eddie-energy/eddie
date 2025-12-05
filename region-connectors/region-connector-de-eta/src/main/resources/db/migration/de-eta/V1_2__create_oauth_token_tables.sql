-- Create OAuth token table for DE-ETA region connector
CREATE TABLE IF NOT EXISTS de_eta.oauth_token (
  id BIGSERIAL PRIMARY KEY,
  connection_id VARCHAR NOT NULL,
  access_token TEXT NOT NULL,
  refresh_token TEXT,
  expires_at TIMESTAMP,
  scopes TEXT,
  created_at TIMESTAMP DEFAULT now(),
  updated_at TIMESTAMP DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_oauth_token_connection ON de_eta.oauth_token(connection_id);

-- Create OAuth state table for DE-ETA region connector
CREATE TABLE IF NOT EXISTS de_eta.oauth_state (
  state VARCHAR PRIMARY KEY,
  permission_id UUID NOT NULL,
  connection_id VARCHAR,
  created_at TIMESTAMP DEFAULT now(),
  expires_at TIMESTAMP
);
