--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

UPDATE permission
SET status = 'WAITING_FOR_CONNECTION'
WHERE status = 'WAITING_FOR_START';

UPDATE permission
SET status = 'CONNECTED'
WHERE status = 'STREAMING_DATA';

UPDATE permission
SET status = 'NOT_CONNECTED'
WHERE status = 'FAILED_TO_START';
