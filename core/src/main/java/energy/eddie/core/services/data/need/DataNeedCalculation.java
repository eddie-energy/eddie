package energy.eddie.core.services.data.need;

import energy.eddie.api.agnostic.Granularity;
import jakarta.annotation.Nullable;

import java.util.List;

public record DataNeedCalculation(boolean supportsDataNeed,
                                  @Nullable List<Granularity> granularities,
                                  @Nullable Timeframe permissionTimeframe,
                                  @Nullable Timeframe energyDataTimeframe) {
    public DataNeedCalculation(boolean supportsDataNeed) {
        this(supportsDataNeed, null, null, null);
    }
}
