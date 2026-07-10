--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

CREATE TABLE connection_limit
(
    id             BIGSERIAL PRIMARY KEY,
    permission_id  UUID        NOT NULL REFERENCES permission (permission_id),
    meter_id       TEXT,
    interval_start TIMESTAMPTZ NOT NULL,
    interval_end   TIMESTAMPTZ NOT NULL,
    min_limit_kw   DECIMAL     NOT NULL,
    max_limit_kw   DECIMAL     NOT NULL,
    UNIQUE (permission_id, interval_start)
);

CREATE INDEX idx_connection_limit_meter_interval_start
    ON connection_limit (meter_id, interval_start);
