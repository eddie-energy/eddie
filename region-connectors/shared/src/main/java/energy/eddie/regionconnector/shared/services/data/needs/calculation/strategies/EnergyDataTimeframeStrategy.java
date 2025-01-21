package energy.eddie.regionconnector.shared.services.data.needs.calculation.strategies;

import energy.eddie.api.agnostic.data.needs.Timeframe;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.DataNeed;
import jakarta.annotation.Nullable;

import java.time.ZonedDateTime;

@FunctionalInterface
public interface EnergyDataTimeframeStrategy {
    /**
     * Creates a timeframe for in which the energy data should lie in.
     * Can return null if the provided data need does not require an energy data timeframe.
     *
     * @param dataNeed          The data need is the basis for the timeframe calculation
     * @param referenceDateTime The reference datetime has to be used to calculate the start and end date. Usually the created datetime of the {@link energy.eddie.api.agnostic.process.model.PermissionRequest}.
     * @return The timeframe of the energy data.
     * @throws UnsupportedDataNeedException if the strategy does not support the data need.
     */
    @Nullable
    Timeframe energyDataTimeframe(
            DataNeed dataNeed,
            ZonedDateTime referenceDateTime
    ) throws UnsupportedDataNeedException;
}
