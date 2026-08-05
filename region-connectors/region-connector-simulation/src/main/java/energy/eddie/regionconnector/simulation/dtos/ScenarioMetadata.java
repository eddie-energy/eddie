// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.dtos;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public record ScenarioMetadata(String connectionId, String permissionId, String dataNeedId,
                               ZonedDateTime creationDateTime) {
    public ScenarioMetadata {
        if (creationDateTime == null) {
            creationDateTime = ZonedDateTime.now(ZoneOffset.UTC);
        }
    }
}
