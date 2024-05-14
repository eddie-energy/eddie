package energy.eddie.api.agnostic.data.needs;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.utils.Pair;
import jakarta.annotation.Nullable;

import java.time.LocalDate;
import java.util.List;

/**
 * Used to by a region-connector to calculate start and end data of a data need, as well as reporting if it supports a
 * certain data need.
 */
public interface DataNeedCalculationService<T extends DataNeedInterface> {

    /**
     * Determines if the region-connector supports this data need type.
     *
     * @param dataNeed the data need, which should be checked
     * @return if the data need is supported
     */
    boolean supportsDataNeed(T dataNeed);

    /**
     * Calculates a list of supported granularities based on a data need. If the data need is not a timeframed data
     * need, an empty list is returned.
     *
     * @param dataNeed the data need
     * @return a list of supported granularities, which can be empty if the data need is not a timeframed data need.
     */
    List<Granularity> supportedGranularities(T dataNeed);

    /**
     * Calculates the start and end date of the permission request.
     *
     * @param dataNeed the data need
     * @return a pair with the start and end date of the permission
     */
    Pair<LocalDate, LocalDate> calculatePermissionStartAndEndDate(T dataNeed);

    /**
     * Calculates the start and end date of the energy data, if the data need requires energy data. Otherwise, it
     * returns null.
     *
     * @param dataNeed the data need
     * @return start and end date of the energy data.
     */
    @Nullable
    Pair<LocalDate, LocalDate> calculateEnergyDataStartAndEndDate(T dataNeed);

    /**
     * The id of the region-connector that provides the implementation of this service.
     *
     * @return region-connector id
     */
    String regionConnectorId();
}
