package energy.eddie.api.agnostic;

/**
 * Enumeration for all types of data that can be retrieved through EDDIE.
 * <p>
 * See <a href="https://architecture.eddie.energy/framework/2-integrating/data-needs.html">DataType in logical data model</a>
 */
public enum DataType {
    HISTORICAL_VALIDATED_CONSUMPTION_DATA,
    FUTURE_VALIDATED_CONSUMPTION_DATA,
    AIIDA_NEAR_REALTIME_DATA,
    ACCOUNTING_POINT_MASTER_DATA,
}
