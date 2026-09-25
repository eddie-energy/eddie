--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

ALTER TABLE permission
    ADD COLUMN monitoring_data_source_id uuid REFERENCES data_source (id) ON DELETE SET NULL;

CREATE INDEX idx_permission_monitoring_data_source
    ON permission (monitoring_data_source_id);
