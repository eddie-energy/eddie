package energy.eddie.regionconnector.es.datadis.utils;

import java.time.ZoneId;

public class DatadisSpecificConstants {
    public static final ZoneId ZONE_ID_SPAIN = ZoneId.of("Europe/Madrid");
    public static final String COUNTRY_CODE = "es";
    public static final String MDA_CODE = COUNTRY_CODE + "-datadis";
    public static final String MDA_DISPLAY_NAME = "Spain Datadis";
    public static final String BASE_PATH = "/region-connectors/es-datadis/";
    public static final int COVERED_METERING_POINTS = 30234170;
    public static final int MAXIMUM_MONTHS_IN_THE_PAST = 24;

    private DatadisSpecificConstants() {
    }
}
