--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

ALTER TABLE status_messages
    ADD COLUMN creation_date VARCHAR(255),
    ADD COLUMN end_date      VARCHAR(255),
    ADD COLUMN reason        TEXT;

UPDATE status_messages
SET creation_date = start_date;