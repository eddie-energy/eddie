--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

-- Connection states are no longer persisted as permission status, but derived at runtime.
UPDATE permission
SET status = 'ACCEPTED'
WHERE status IN ('WAITING_FOR_CONNECTION', 'CONNECTED', 'NOT_CONNECTED');
