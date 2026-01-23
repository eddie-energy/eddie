// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors.permission;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class LatestPermissionRecordNotFoundException extends Exception {
    public LatestPermissionRecordNotFoundException(UUID permissionId) {
        super("Latest permission record not found for permission: %s".formatted(permissionId));
    }
}
