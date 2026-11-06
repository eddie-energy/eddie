-- SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
-- SPDX-License-Identifier: Apache-2.0

CREATE TABLE rest.validated_historical_data_marked_document_v1_12
(
    id serial PRIMARY KEY,
    inserted_at timestamp with time zone NOT NULL DEFAULT NOW(),
    payload jsonb NOT NULL
);
