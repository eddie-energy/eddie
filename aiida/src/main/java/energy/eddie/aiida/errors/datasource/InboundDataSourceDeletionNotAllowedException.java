// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors.datasource;

import java.util.UUID;

public class InboundDataSourceDeletionNotAllowedException extends Exception {
    public InboundDataSourceDeletionNotAllowedException(UUID dataSourceId) {
        super("Inbound data source %s cannot be deleted, it is removed when its permission ends.".formatted(dataSourceId));
    }
}
