--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

CREATE TABLE energy_community_data_need
(
    data_need_id         varchar(36)                 NOT NULL PRIMARY KEY,
    created_at           timestamp(6) WITH TIME ZONE NOT NULL,
    description          text                        NOT NULL,
    name                 text                        NOT NULL,
    policy_link          text                        NOT NULL,
    purpose              text                        NOT NULL,
    participation_factor double precision            NOT NULL
);
