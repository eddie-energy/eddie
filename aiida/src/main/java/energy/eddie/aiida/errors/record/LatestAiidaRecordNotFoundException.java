// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors.record;

import java.util.UUID;

public class LatestAiidaRecordNotFoundException extends Exception {
    public LatestAiidaRecordNotFoundException(UUID dataSourceId) {
        super("Latest Aiida Record not found for data source with ID: %s".formatted(dataSourceId));
    }
}
