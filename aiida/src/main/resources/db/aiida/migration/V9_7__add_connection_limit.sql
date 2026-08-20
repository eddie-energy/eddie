--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

CREATE TABLE connection_limit
(
    permission_id   uuid        NOT NULL REFERENCES permission (permission_id),
    meter_id        TEXT        NOT NULL DEFAULT '',
    interval_start  timestamptz NOT NULL,
    interval_end    timestamptz NOT NULL,
    min_limit_kw    DECIMAL     NOT NULL,
    max_limit_kw    DECIMAL     NOT NULL,
    mrid            TEXT        NOT NULL,
    revision_number INTEGER     NOT NULL,
    created_at      timestamptz NOT NULL,
    PRIMARY KEY (permission_id, meter_id, interval_start, interval_end),
    CONSTRAINT connection_limit_valid_interval CHECK (interval_start < interval_end)
);

CREATE INDEX idx_connection_limit_meter_interval_start
    ON connection_limit (permission_id, meter_id, interval_start);

CREATE INDEX idx_connection_limit_mrid
    ON connection_limit (mrid);

CREATE TABLE connection_limit_default
(
    permission_id        uuid NOT NULL REFERENCES permission (permission_id),
    default_min_limit_kw DECIMAL,
    default_max_limit_kw DECIMAL,
    PRIMARY KEY (permission_id)
);