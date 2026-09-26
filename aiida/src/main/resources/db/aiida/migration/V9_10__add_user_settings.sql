--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

CREATE TABLE user_settings
(
    user_id       uuid NOT NULL,
    contact_email TEXT,
    PRIMARY KEY (user_id)
);
