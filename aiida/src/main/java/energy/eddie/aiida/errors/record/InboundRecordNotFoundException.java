// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors.record;

import java.util.UUID;

public class InboundRecordNotFoundException extends Exception {
    public InboundRecordNotFoundException(UUID dataSourceId) {
        super("No inbound record found for data source with ID '%s'.".formatted(dataSourceId));
    }
}
