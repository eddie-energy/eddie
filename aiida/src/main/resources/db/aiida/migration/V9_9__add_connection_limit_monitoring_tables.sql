--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

ALTER TABLE permission
    ADD COLUMN monitoring_data_source_id uuid REFERENCES data_source (id) ON DELETE SET NULL;

CREATE INDEX idx_permission_monitoring_data_source
    ON permission (monitoring_data_source_id);

CREATE TABLE user_settings
(
    user_id       uuid NOT NULL,
    contact_email TEXT,
    PRIMARY KEY (user_id)
);

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
