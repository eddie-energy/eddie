--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

ALTER TABLE connection_limit_violation
    ADD COLUMN lowest_power_kw  DECIMAL,
    ADD COLUMN highest_power_kw DECIMAL;

UPDATE connection_limit_violation
   SET lowest_power_kw  = LEAST(start_power_kw, peak_power_kw),
       highest_power_kw = GREATEST(start_power_kw, peak_power_kw);

ALTER TABLE connection_limit_violation
    ALTER COLUMN lowest_power_kw SET NOT NULL,
    ALTER COLUMN highest_power_kw SET NOT NULL,
    DROP COLUMN peak_power_kw;
