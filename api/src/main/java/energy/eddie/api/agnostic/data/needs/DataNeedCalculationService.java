package energy.eddie.api.agnostic.data.needs;

/**
 * Used to by a region-connector to calculate start and end data of a data need, as well as reporting if it supports a
 * certain data need.
 */
public interface DataNeedCalculationService<T extends DataNeedInterface> {
    /**
     * Calculates relevant information for a data need
     * @param dataNeed the data need
     * @return the calculation outcomes of the data need
     */
    DataNeedCalculation calculate(T dataNeed);

    /**
     * The id of the region-connector that provides the implementation of this service.
     *
     * @return region-connector id
     */
    String regionConnectorId();
}
