package energy.eddie.regionconnector.shared.services.data.needs.calculation.strategies;

import energy.eddie.api.agnostic.data.needs.Timeframe;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.TimeframedDataNeed;
import energy.eddie.dataneeds.utils.TimeframedDataNeedUtils;
import jakarta.annotation.Nullable;

import java.time.ZonedDateTime;

public class DefaultEnergyDataTimeframeStrategy implements EnergyDataTimeframeStrategy {
    private final RegionConnectorMetadata regionConnectorMetadata;

    public DefaultEnergyDataTimeframeStrategy(RegionConnectorMetadata regionConnectorMetadata) {
        this.regionConnectorMetadata = regionConnectorMetadata;
    }

    /**
     * Calculates the start and end date of the energy data, if the data need requires energy data.
     * Otherwise, it returns null.
     *
     * @param dataNeed          the data need
     * @param referenceDateTime the reference datetime that is used to calculate the timeframe.
     * @return start and end date of the energy data.
     * @throws UnsupportedDataNeedException if the data need is not meant for energy data
     */
    @Override
    @Nullable
    public Timeframe energyDataTimeframe(
            DataNeed dataNeed,
            ZonedDateTime referenceDateTime
    ) throws UnsupportedDataNeedException {
        if (!(dataNeed instanceof TimeframedDataNeed timeframedDataNeed)) {
            return null;
        }
        var now = referenceDateTime.toLocalDate();
        var wrapper = TimeframedDataNeedUtils.calculateRelativeStartAndEnd(
                timeframedDataNeed,
                now,
                regionConnectorMetadata.earliestStart(),
                regionConnectorMetadata.latestEnd()
        );
        return new Timeframe(wrapper.calculatedStart(), wrapper.calculatedEnd());
    }
}
