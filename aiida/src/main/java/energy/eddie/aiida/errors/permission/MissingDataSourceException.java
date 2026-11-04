// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors.permission;

import java.util.UUID;

public class MissingDataSourceException extends Exception {
    public MissingDataSourceException(UUID permissionId) {
        super("A data source is required to accept the outbound permission '%s'.".formatted(permissionId));
    }
}
