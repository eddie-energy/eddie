package energy.eddie.regionconnector.shared.utils;

public class CommonPaths {
    public static final String ALL_REGION_CONNECTORS_BASE_URL_PATH = "region-connectors";
    public static final String CE_FILE_NAME = "ce.js";

    private CommonPaths() {}

    public static String getServletPathForRegionConnector(String regionConnectorId) {
        return "/%s/%s/*".formatted(ALL_REGION_CONNECTORS_BASE_URL_PATH, regionConnectorId);
    }

    public static String getClasspathForCeElement(String regionConnectorName) {
        return "/public/%s/%s/%s".formatted(ALL_REGION_CONNECTORS_BASE_URL_PATH,
                                            regionConnectorName,
                                            CE_FILE_NAME);
    }
}
