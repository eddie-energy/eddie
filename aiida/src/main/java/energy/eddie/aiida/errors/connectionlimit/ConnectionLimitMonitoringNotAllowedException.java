// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors.connectionlimit;

import java.util.UUID;

/**
 * Thrown when attempting to use permission or data source that cannot be used for connection limit monitoring.
 */
public class ConnectionLimitMonitoringNotAllowedException extends Exception {

    public ConnectionLimitMonitoringNotAllowedException(UUID permissionId) {
        super("Permission '%s' does not support connection limit monitoring.".formatted(permissionId));
    }

    public ConnectionLimitMonitoringNotAllowedException(String message) {
        super(message);
    }
}
