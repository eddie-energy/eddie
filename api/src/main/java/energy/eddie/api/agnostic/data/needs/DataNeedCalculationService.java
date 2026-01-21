package energy.eddie.api.agnostic.data.needs;

import java.time.ZonedDateTime;
import java.util.Set;

/**
 * Used to by a region-connector to calculate start and end data of a data need, as well as reporting if it supports a
 * certain data need.
 *
 * @param <T> Should always be {@code DataNeed}, but this is only available in the core itself.
 */
public interface DataNeedCalculationService<T extends DataNeedInterface> {
    /**
     * Calculates relevant information for a data need
     *
     * @param dataNeed the data need
     * @return the calculation results for the data need
     */
    DataNeedCalculationResult calculate(T dataNeed);

    /**
     * Calculates relevant information for a data need, using the reference datetime
     *
     * @param dataNeed          the data need
     * @param referenceDateTime the reference datetime the calculations are based on
     * @return the calculation results for the data need
     */
    DataNeedCalculationResult calculate(T dataNeed, ZonedDateTime referenceDateTime);

    /**
     * Calculate relevant information for a given data need ID
     *
     * @param dataNeedId the ID of the data need
     * @return the calculation results for the data need
     */
    DataNeedCalculationResult calculate(String dataNeedId);

    /**
     * Calculates relevant information for given data need ID, using the reference datetime
     *
     * @param dataNeedId        the ID of the data need
     * @param referenceDateTime the reference datetime the calculations are based on
     * @return the calculation results for the data need
     */
    DataNeedCalculationResult calculate(String dataNeedId, ZonedDateTime referenceDateTime);

    /**
     * Calculates the relevant information for multiple given data need IDs.
     *
     * @param dataNeedIds the ID of the data needs
     * @return a Map of the calculations, where the key is the data need ID and the value the calculation result.
     */
    MultipleDataNeedCalculationResult calculateAll(Set<String> dataNeedIds);

    /**
     * Calculates the relevant information for multiple given data need IDs, using the reference datetime.
     *
     * @param dataNeedIds       the ID of the data needs
     * @param referenceDateTime the reference datetime the calculations are based on
     * @return a Map of the calculations, where the key is the data need ID and the value the calculation result.
     */
    MultipleDataNeedCalculationResult calculateAll(Set<String> dataNeedIds, ZonedDateTime referenceDateTime);

    /**
     * The id of the region-connector that provides the implementation of this service.
     *
     * @return region-connector id
     */
    String regionConnectorId();
}
