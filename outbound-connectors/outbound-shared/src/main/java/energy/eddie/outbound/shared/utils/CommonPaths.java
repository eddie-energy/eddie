package energy.eddie.outbound.shared.utils;

public class CommonPaths {
    public static final String ALL_OUTBOUND_CONNECTORS_BASE_URL_PATH = "outbound-connectors";

    private CommonPaths() {}

    public static String getServletPathForOutboundConnector(String outboundConnectorId) {
        return "/%s/%s/*".formatted(ALL_OUTBOUND_CONNECTORS_BASE_URL_PATH, outboundConnectorId);
    }
}
