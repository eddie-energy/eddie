// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors.permission;

import java.util.UUID;

public class DetailFetchingFailedException extends Exception {
    public DetailFetchingFailedException(UUID permissionId) {
        super("Failed to fetch permission details or MQTT credentials for permission '%s'".formatted(permissionId));
    }
}
