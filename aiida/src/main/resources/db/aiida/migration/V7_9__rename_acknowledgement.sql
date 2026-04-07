--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

ALTER TABLE aiida_local_data_need
    RENAME COLUMN is_acknowledgement_required TO acknowledgement_required;