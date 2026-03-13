--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

CREATE TABLE cesu_join_request_data_need
(
    data_need_id         varchar(36)                 NOT NULL PRIMARY KEY,
    created_at           timestamp(6) WITH TIME ZONE NOT NULL,
    description          text                        NOT NULL,
    name                 text                        NOT NULL,
    policy_link          text                        NOT NULL,
    purpose              text                        NOT NULL,
    enabled         bool        NOT NULL DEFAULT TRUE,
    max_granularity varchar(15) NOT NULL,
    min_granularity varchar(15) NOT NULL,
    participation_factor int         NOT NULL
        CHECK ( participation_factor BETWEEN 1 AND 100),
    energy_direction     varchar(11) NOT NULL
        CHECK ( energy_direction IN ('CONSUMPTION', 'PRODUCTION') )
);
