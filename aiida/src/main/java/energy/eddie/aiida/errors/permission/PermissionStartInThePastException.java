// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors.permission;

import java.time.ZonedDateTime;
import java.util.UUID;

public class PermissionStartInThePastException extends Exception {
    public PermissionStartInThePastException(UUID permissionId, ZonedDateTime start) {
        super("Permission %s cannot be fulfilled: start date %s is in the past.".formatted(permissionId, start));
    }
}
