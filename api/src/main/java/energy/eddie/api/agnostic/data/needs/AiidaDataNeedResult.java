// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic.data.needs;

/**
 * Result type for the calculation of AIIDA data-needs.
 *
 * @param supportsAllSchemas Indicates whether all given schemas by the data-need are supported by its type.
 * @param energyTimeframe    The start and end date of the requested data.
 */
public record AiidaDataNeedResult(
        boolean supportsAllSchemas,
        Timeframe energyTimeframe
) implements DataNeedCalculationResult {
}
