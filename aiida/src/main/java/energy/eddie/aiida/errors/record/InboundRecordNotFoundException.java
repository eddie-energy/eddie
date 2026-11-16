// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors.record;

import java.util.UUID;

public class InboundRecordNotFoundException extends Exception {
    public InboundRecordNotFoundException(UUID permissionId) {
        super("No inbound record found for permission ('%s'), permission has no data source.".formatted(permissionId));
    }

    public InboundRecordNotFoundException(UUID permissionId, UUID dataSourceId) {
        super("No inbound record found for permission ('%s') with data source ('%s').".formatted(permissionId, dataSourceId));
    }
}
