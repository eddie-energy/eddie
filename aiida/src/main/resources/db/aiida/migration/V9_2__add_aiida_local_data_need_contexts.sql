--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

CREATE TABLE aiida_local_data_need_contexts
(
    data_need_id uuid NOT NULL,
    context      text NOT NULL,
    PRIMARY KEY (data_need_id, context),
    FOREIGN KEY (data_need_id) REFERENCES aiida_local_data_need (data_need_id)
);