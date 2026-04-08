// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic.data.needs;

import energy.eddie.api.agnostic.Granularity;

import java.util.List;
import java.util.Optional;

public record CESUJoinRequestDataNeedResult(
        Timeframe permissionTimeframe,
        Timeframe energyDataTimeframe,
        List<Granularity> supportedGranularities,
        Optional<EnergyDirection> energyDirection,
        Optional<Integer> participationFactor
) implements DataNeedCalculationResult {
    public CESUJoinRequestDataNeedResult(Timeframe timeframe, List<Granularity> supportedGranularities) {
        this(timeframe, timeframe, supportedGranularities, Optional.empty(), Optional.empty());
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public CESUJoinRequestDataNeedResult(
            Timeframe timeframe,
            List<Granularity> supportedGranularities,
            Optional<EnergyDirection> energyDirection,
            Optional<Integer> participationFactor
    ) {
        this(timeframe, timeframe, supportedGranularities, energyDirection, participationFactor);
    }
}
