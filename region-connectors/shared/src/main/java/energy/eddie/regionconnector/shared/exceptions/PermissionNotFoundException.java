// SPDX-FileCopyrightText: 2023-2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.exceptions;

public class PermissionNotFoundException extends Exception {
    public PermissionNotFoundException(String permissionId) {
        super("No permission with ID '%s' found.".formatted(permissionId));
    }
}
