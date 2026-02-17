// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic.data.needs;

public sealed interface DataNeedCalculationResult permits
        AccountingPointDataNeedResult,
        AiidaDataNeedResult,
        DataNeedNotFoundResult,
        DataNeedNotSupportedResult,
        ValidatedHistoricalDataDataNeedResult {}
