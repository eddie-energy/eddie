--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

CREATE TABLE aiida_data_need_assets
(
    data_need_id varchar(36) NOT NULL,
    asset        text        NOT NULL,
    PRIMARY KEY (data_need_id, asset)
);

INSERT INTO aiida_data_need_assets (data_need_id, asset)
SELECT data_need_id, asset
FROM outbound_aiida_data_need
UNION ALL
SELECT data_need_id, asset
FROM inbound_aiida_data_need;

ALTER TABLE outbound_aiida_data_need
    DROP COLUMN asset;
ALTER TABLE inbound_aiida_data_need
    DROP COLUMN asset;
