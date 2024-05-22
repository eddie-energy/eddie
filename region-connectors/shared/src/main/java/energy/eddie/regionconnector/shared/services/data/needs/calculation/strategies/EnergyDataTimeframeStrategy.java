package energy.eddie.regionconnector.shared.services.data.needs.calculation.strategies;

import energy.eddie.api.agnostic.data.needs.Timeframe;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.DataNeed;
import jakarta.annotation.Nullable;

@FunctionalInterface
public interface EnergyDataTimeframeStrategy {
    @Nullable
    Timeframe energyDataTimeframe(DataNeed dataNeed) throws UnsupportedDataNeedException;
}
