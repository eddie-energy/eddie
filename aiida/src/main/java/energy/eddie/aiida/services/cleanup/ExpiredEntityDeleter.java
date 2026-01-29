// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.cleanup;

import java.time.Instant;

@FunctionalInterface
public interface ExpiredEntityDeleter {
    int deleteOldestByTimestampBefore(Instant threshold, int limit);
}
