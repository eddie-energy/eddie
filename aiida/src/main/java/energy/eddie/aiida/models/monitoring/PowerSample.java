// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.monitoring;

import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.api.agnostic.aiida.UnitOfMeasurement;

import java.time.Instant;

public record PowerSample(
        long recordId,
        Instant timestamp,
        ObisCode dataTag,
        String value,
        UnitOfMeasurement unitOfMeasurement
) {
}
