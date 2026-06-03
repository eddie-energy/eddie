--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

CREATE TABLE aiida_data_need_permission_commands
(
    data_need_id       varchar(36) NOT NULL,
    permission_command text        NOT NULL,
    PRIMARY KEY (data_need_id, permission_command)
);