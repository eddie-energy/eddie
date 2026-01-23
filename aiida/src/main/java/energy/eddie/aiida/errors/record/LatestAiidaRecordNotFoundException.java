// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors.record;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class LatestAiidaRecordNotFoundException extends Exception {
    public LatestAiidaRecordNotFoundException(UUID dataSourceId) {
        super("Latest Aiida Record not found for data source with ID: %s".formatted(dataSourceId));
    }
}
