--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

CREATE TABLE rest.energy_sharing_reference_data_market_document
(
    id          serial                   NOT NULL,
    inserted_at timestamp with time zone NOT NULL DEFAULT NOW(),
    payload     jsonb                    NOT NULL,
    PRIMARY KEY (id)
);