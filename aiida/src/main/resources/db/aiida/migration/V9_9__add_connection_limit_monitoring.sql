--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

CREATE TABLE connection_limit_monitoring
(
    permission_id  uuid NOT NULL REFERENCES permission (permission_id),
    data_source_id uuid NOT NULL REFERENCES data_source (id) ON DELETE CASCADE,
    PRIMARY KEY (permission_id)
);

CREATE INDEX idx_connection_limit_monitoring_data_source
    ON connection_limit_monitoring (data_source_id);
