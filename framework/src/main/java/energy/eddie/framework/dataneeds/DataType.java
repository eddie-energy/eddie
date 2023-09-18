package energy.eddie.framework.dataneeds;

/**
 * Enumeration for all types of data that can be retrieved through the EDDIE framework.
 * <p>
 * See <a href="https://eddie-web.projekte.fh-hagenberg.at/docs/requirements/4_data_requirements/1_logical_data_model/">DataType in logical data model</a>
 */
public enum DataType {
    HISTORICAL_VALIDATED_CONSUMPTION_DATA,
    FUTURE_VALIDATED_CONSUMPTION_DATA,
    SMART_METER_P1_DATA,
    ACCOUNTING_POINT_MASTER_DATA,
}
