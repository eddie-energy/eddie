package energy.eddie.regionconnector.be.fluvius.data.needs;

import energy.eddie.api.agnostic.data.needs.Timeframe;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.utils.TimeframedDataNeedUtils;
import energy.eddie.regionconnector.shared.services.data.needs.calculation.strategies.EnergyDataTimeframeStrategy;
import jakarta.annotation.Nullable;

import java.time.ZonedDateTime;

public class FluviusEnergyTimeframeStrategy implements EnergyDataTimeframeStrategy {
    private final RegionConnectorMetadata regionConnectorMetadata;

    public FluviusEnergyTimeframeStrategy(RegionConnectorMetadata regionConnectorMetadata) {this.regionConnectorMetadata = regionConnectorMetadata;}

    @Nullable
    @Override
    public Timeframe energyDataTimeframe(
            DataNeed dataNeed,
            ZonedDateTime referenceDateTime
    ) throws UnsupportedDataNeedException {
        var now = referenceDateTime.toLocalDate();
        if (!(dataNeed instanceof ValidatedHistoricalDataDataNeed vhdDataNeed)) {
            return new Timeframe(now, now.plusDays(1));
        }
        var wrapper = TimeframedDataNeedUtils.calculateRelativeStartAndEnd(
                vhdDataNeed,
                now,
                regionConnectorMetadata.earliestStart(),
                regionConnectorMetadata.latestEnd()
        );
        return new Timeframe(wrapper.calculatedStart(), wrapper.calculatedEnd().plusDays(1));
    }
}
