--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

ALTER TABLE permission
    ADD COLUMN min_limit_kw DECIMAL NULL,
    ADD COLUMN max_limit_kw DECIMAL NULL;

UPDATE permission p
SET
    min_limit_kw = d.default_min_limit_kw,
    max_limit_kw = d.default_max_limit_kw
FROM connection_limit_default d
WHERE p.permission_id = d.permission_id;

DROP TABLE connection_limit_default;