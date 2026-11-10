--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

-- Outbound data needs may allow several assets, inbound data needs have none.
CREATE TABLE aiida_local_data_need_assets
(
    data_need_id uuid NOT NULL,
    asset        text NOT NULL,
    PRIMARY KEY (data_need_id, asset),
    FOREIGN KEY (data_need_id) REFERENCES aiida_local_data_need (data_need_id)
);

INSERT INTO aiida_local_data_need_assets (data_need_id, asset)
SELECT data_need_id, asset
FROM aiida_local_data_need
WHERE type = 'outbound-aiida';

ALTER TABLE aiida_local_data_need
    DROP COLUMN asset;

-- Inbound data sources receive data of any asset, so they no longer have an asset.
ALTER TABLE data_source
    ALTER COLUMN asset DROP NOT NULL;

UPDATE data_source
SET asset = NULL
WHERE type = 'INBOUND';

-- Every other data source still requires an asset.
ALTER TABLE data_source
    ADD CONSTRAINT data_source_asset_check CHECK ((type = 'INBOUND') = (asset IS NULL));
