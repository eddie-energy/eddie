// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors.permission;

public class PermissionUnfulfillableException extends Exception {
    public PermissionUnfulfillableException(String serviceName) {
        super("Permission for service '%s' cannot be fulfilled by your AIIDA.".formatted(serviceName));
    }
}
