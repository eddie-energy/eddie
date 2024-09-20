package energy.eddie.api.agnostic.data.needs;

import energy.eddie.api.agnostic.Granularity;
import jakarta.annotation.Nullable;

import java.util.List;

public record DataNeedCalculation(boolean supportsDataNeed,
                                  @Nullable String unsupportedDataNeedMessage,
                                  @Nullable List<Granularity> granularities,
                                  @Nullable Timeframe permissionTimeframe,
                                  @Nullable Timeframe energyDataTimeframe) {
    public DataNeedCalculation(boolean supportsDataNeed, String unsupportedDataNeedMessage) {
        this(supportsDataNeed, unsupportedDataNeedMessage, null, null, null);
    }

    public DataNeedCalculation(
            boolean supportsDataNeed,
            @Nullable List<Granularity> granularities,
            @Nullable Timeframe permissionTimeframe,
            @Nullable Timeframe energyDataTimeframe
    ) {
        this(supportsDataNeed, null, granularities, permissionTimeframe, energyDataTimeframe);
    }
}
