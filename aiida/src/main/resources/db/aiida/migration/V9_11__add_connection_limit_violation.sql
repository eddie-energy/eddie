--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

CREATE TABLE connection_limit_violation
(
    id             bigserial PRIMARY KEY,
    permission_id  uuid        NOT NULL REFERENCES permission (permission_id) ON DELETE CASCADE,
    data_source_id uuid        NOT NULL REFERENCES data_source (id) ON DELETE CASCADE,
    started_at     timestamptz NOT NULL,
    ended_at       timestamptz,
    document_id    TEXT,
    min_limit_kw   DECIMAL,
    max_limit_kw   DECIMAL,
    start_power_kw DECIMAL     NOT NULL,
    peak_power_kw  DECIMAL     NOT NULL
);

CREATE INDEX idx_connection_limit_violation_permission_started_at
    ON connection_limit_violation (permission_id, started_at);

CREATE INDEX idx_connection_limit_violation_open
    ON connection_limit_violation (permission_id) WHERE ended_at IS NULL;
