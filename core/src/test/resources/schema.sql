-- SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
-- SPDX-License-Identifier: Apache-2.0

CREATE SCHEMA IF NOT EXISTS core;

CREATE TABLE IF NOT EXISTS core.eddie_application_information
(
    eddie_id uuid PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);