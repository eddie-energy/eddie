// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic.data.needs;

import energy.eddie.dataneeds.needs.AccountingPointDataNeed;

/**
 * The result for accounting point data needs
 *
 * @param permissionTimeframe the start and end date from which data can be requested.
 */
public record AccountingPointDataNeedResult(Timeframe permissionTimeframe,
                                            AccountingPointDataNeed dataNeed) implements DataNeedCalculationResult.DataNeedCalculationSuccessResult<AccountingPointDataNeed> {
}
