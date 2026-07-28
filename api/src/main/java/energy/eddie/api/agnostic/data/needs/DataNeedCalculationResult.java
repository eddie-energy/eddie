// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic.data.needs;

import energy.eddie.dataneeds.needs.DataNeed;

public sealed interface DataNeedCalculationResult permits DataNeedCalculationResult.DataNeedCalculationSuccessResult, DataNeedNotFoundResult, DataNeedNotSupportedResult {

    sealed interface DataNeedCalculationSuccessResult<T extends DataNeed> extends DataNeedCalculationResult permits AccountingPointDataNeedResult, AiidaDataNeedResult, CESUJoinRequestDataNeedResult, ValidatedHistoricalDataDataNeedResult {
        T dataNeed();
    }
}
