package energy.eddie.regionconnector.at.eda.data.needs.calculation.strategies;

import energy.eddie.api.agnostic.data.needs.Timeframe;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.TimeframedDataNeed;
import energy.eddie.dataneeds.utils.DataNeedWrapper;
import energy.eddie.dataneeds.utils.TimeframedDataNeedUtils;
import energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata;
import energy.eddie.regionconnector.shared.services.data.needs.calculation.strategies.EnergyDataTimeframeStrategy;
import jakarta.annotation.Nullable;

import java.time.LocalDate;

public class EdaStrategy implements EnergyDataTimeframeStrategy {
    /**
     * Calculates the start and end date of the energy data, if the data need requires energy data. Otherwise, it
     * returns null.
     *
     * @param dataNeed the data need
     * @return start and end date of the energy data.
     * @throws UnsupportedDataNeedException if the data need is not meant for energy data
     */
    @Override
    @Nullable
    public Timeframe energyDataTimeframe(DataNeed dataNeed) throws UnsupportedDataNeedException {
        if (!(dataNeed instanceof TimeframedDataNeed timeframedDataNeed)) {
            return null;
        }
        var now = LocalDate.now(EdaRegionConnectorMetadata.AT_ZONE_ID);
        DataNeedWrapper wrapper = TimeframedDataNeedUtils.calculateRelativeStartAndEnd(
                timeframedDataNeed,
                now,
                EdaRegionConnectorMetadata.PERIOD_EARLIEST_START,
                EdaRegionConnectorMetadata.PERIOD_LATEST_END
        );

        if (!isCompletelyInThePastOrInTheFuture(wrapper, now)) {
            throw new UnsupportedDataNeedException(dataNeed.id(),
                                                   "Data need must be completely in the past or in the future");
        }

        return new Timeframe(wrapper.calculatedStart(), wrapper.calculatedEnd());
    }

    private boolean isCompletelyInThePastOrInTheFuture(DataNeedWrapper wrapper, LocalDate now) {
        LocalDate start = wrapper.calculatedStart();
        LocalDate end = wrapper.calculatedEnd();

        return end.isBefore(now)
               || isPresent(start, now)
               || isFuture(start, now);
    }

    private boolean isPresent(LocalDate start, LocalDate now) {
        return start.isEqual(now);
    }

    private boolean isFuture(LocalDate start, LocalDate now) {
        return start.isAfter(now);
    }
}
