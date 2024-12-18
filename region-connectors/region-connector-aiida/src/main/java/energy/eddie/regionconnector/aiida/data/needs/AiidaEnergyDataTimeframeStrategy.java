package energy.eddie.regionconnector.aiida.data.needs;

import energy.eddie.api.agnostic.data.needs.Timeframe;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.TimeframedDataNeed;
import energy.eddie.dataneeds.needs.aiida.AiidaDataNeed;
import energy.eddie.dataneeds.utils.DataNeedWrapper;
import energy.eddie.dataneeds.utils.TimeframedDataNeedUtils;
import energy.eddie.regionconnector.aiida.AiidaRegionConnectorMetadata;
import energy.eddie.regionconnector.shared.services.data.needs.calculation.strategies.EnergyDataTimeframeStrategy;
import jakarta.annotation.Nullable;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;

import static energy.eddie.regionconnector.aiida.AiidaRegionConnectorMetadata.EARLIEST_START;
import static energy.eddie.regionconnector.aiida.AiidaRegionConnectorMetadata.LATEST_END;

public class AiidaEnergyDataTimeframeStrategy implements EnergyDataTimeframeStrategy {
    private final Clock clock;

    public AiidaEnergyDataTimeframeStrategy(Clock clock) {this.clock = clock;}

    @Nullable
    @Override
    public Timeframe energyDataTimeframe(DataNeed dataNeed) throws UnsupportedDataNeedException {
        if (!(dataNeed instanceof AiidaDataNeed aiidaDataNeed)) {
            throw new UnsupportedDataNeedException(AiidaRegionConnectorMetadata.REGION_CONNECTOR_ID,
                                                   dataNeed.id(),
                                                   "Unsupported data need");
        }
        var today = LocalDate.now(clock);
        var wrapper = TimeframedDataNeedUtils.calculateRelativeStartAndEnd(
                aiidaDataNeed,
                today,
                // in case of open end/start, fixed values are used
                EARLIEST_START,
                LATEST_END
        );
        return new Timeframe(
                getAppropriateStartDate(wrapper),
                getAppropriateEndDate(wrapper)
        );
    }


    /**
     * Returns the calculated start date from the wrapper or the fixed start date if the data need is using open start.
     *
     * @param wrapper Wrapper as returned from
     *                {@link TimeframedDataNeedUtils#calculateRelativeStartAndEnd(TimeframedDataNeed, LocalDate, Period,
     *                Period)}
     * @return LocalDate to use as start date for the permission request.
     */
    private static LocalDate getAppropriateStartDate(DataNeedWrapper wrapper) {
        if (wrapper.timeframedDataNeed()
                   .duration() instanceof RelativeDuration relativeDuration && relativeDuration.start().isEmpty())
            return LocalDate.of(2000, 1, 1);

        return wrapper.calculatedStart();
    }

    /**
     * Returns the calculated end date from the wrapper or the fixed end date if the data need is using open start.
     *
     * @param wrapper Wrapper as returned from
     *                {@link TimeframedDataNeedUtils#calculateRelativeStartAndEnd(TimeframedDataNeed, LocalDate, Period,
     *                Period)} (String, LocalDate, Period, Period)}
     * @return LocalDate to use as end date for the permission request.
     */
    private static LocalDate getAppropriateEndDate(DataNeedWrapper wrapper) {
        if (wrapper.timeframedDataNeed()
                   .duration() instanceof RelativeDuration relativeDuration && relativeDuration.end().isEmpty())
            return LocalDate.of(9999, 12, 31);

        return wrapper.calculatedEnd();
    }
}
