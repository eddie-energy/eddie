package energy.eddie.regionconnector.es.datadis.utils;

import java.time.ZoneId;

public class DatadisSpecificConstants {
    public static final ZoneId ZONE_ID_SPAIN = ZoneId.of("Europe/Madrid");
    /**
     * Datadis gives access to metering data for a maximum of 24 months in the past.
     */
    public static final int MAXIMUM_MONTHS_IN_THE_PAST = 24;
    /**
     * Datadis can grant permissions for a maximum of 24 months in the future.
     * If no end date is provided, the end date will be set to the current date plus 24 months.
     */
    public static final int MAXIMUM_MONTHS_IN_THE_FUTURE = 24;

    private DatadisSpecificConstants() {
    }
}