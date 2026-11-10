--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

CREATE TABLE aiida_local_data_need_assets
(
    data_need_id uuid NOT NULL,
    asset        text NOT NULL,
    PRIMARY KEY (data_need_id, asset),
    FOREIGN KEY (data_need_id) REFERENCES aiida_local_data_need (data_need_id)
);

INSERT INTO aiida_local_data_need_assets (data_need_id, asset)
SELECT data_need_id, asset
FROM aiida_local_data_need;

ALTER TABLE aiida_local_data_need
    DROP COLUMN asset;
