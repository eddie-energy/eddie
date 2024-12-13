package energy.eddie.api.agnostic.data.needs;

/**
 * Used to by a region-connector to calculate start and end data of a data need, as well as reporting if it supports a
 * certain data need.
 * @param <T> Should always be {@code DataNeed}, but this is only available in the core itself.
 */
public interface DataNeedCalculationService<T extends DataNeedInterface> {
    /**
     * Calculates relevant information for a data need
     * @param dataNeed the data need
     * @return the calculation results for the data need
     */
    DataNeedCalculationResult calculate(T dataNeed);

    /**
     * Calculate relevant information for a given data need ID
     * @param dataNeedId the ID of the data need
     * @return the calculation results for the data need
     */
    DataNeedCalculationResult calculate(String dataNeedId);

    /**
     * The id of the region-connector that provides the implementation of this service.
     *
     * @return region-connector id
     */
    String regionConnectorId();
}
