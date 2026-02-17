// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic.data.needs;

import energy.eddie.api.agnostic.aiida.AiidaSchema;

import java.util.Set;

/**
 * Result type for the calculation of AIIDA data-needs.
 *
 * @param schemas            The schemas that are requested by the data need.
 * @param supportedSchemas   The schemas that are supported by the data need type.
 * @param energyTimeframe    The start and end date of the requested data.
 */
public record AiidaDataNeedResult(
        Set<AiidaSchema> schemas,
        Set<AiidaSchema> supportedSchemas,
        Timeframe energyTimeframe
) implements DataNeedCalculationResult {
    public boolean supportsAllSchemas() {
        for (var schema : schemas) {
            if (!supportedSchemas().contains(schema)) {
                return false;
            }
        }
        return true;
    }
}
