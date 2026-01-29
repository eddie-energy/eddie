// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic.data.needs;

/**
 * The result for accounting point data needs
 *
 * @param permissionTimeframe the start and end date from which data can be requested.
 */
public record AccountingPointDataNeedResult(Timeframe permissionTimeframe) implements DataNeedCalculationResult {
}
