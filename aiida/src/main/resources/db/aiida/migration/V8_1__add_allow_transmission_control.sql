--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

ALTER TABLE aiida_local_data_need
    ADD COLUMN allow_transmission_control BOOLEAN NOT NULL DEFAULT FALSE;