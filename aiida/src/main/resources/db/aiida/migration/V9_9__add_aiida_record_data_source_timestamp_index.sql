--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

-- Supports reading the measurements of one data source within a time interval.
CREATE INDEX IF NOT EXISTS idx_aiida_record__data_source_id_timestamp
    ON aiida_record (data_source_id, timestamp);
