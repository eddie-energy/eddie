package energy.eddie.regionconnector.shared.utils;

public class ServletPathUtil {
    public static final String ALL_REGION_CONNECTORS_BASE_URL_PATH = "region-connectors";

    private ServletPathUtil() {}

    public static String getServletPathForRegionConnector(String regionConnectorId) {
        return "/%s/%s/*".formatted(ALL_REGION_CONNECTORS_BASE_URL_PATH, regionConnectorId);
    }
}
