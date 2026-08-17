--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

ALTER TABLE permission
    ADD COLUMN display_name TEXT NULL;

UPDATE permission
    SET display_name = service_name;
