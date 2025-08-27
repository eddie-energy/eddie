package energy.eddie.outbound.shared.utils;

public class CommonPaths {

    private CommonPaths() {}

    public static String getServletPathForOutboundConnector(String outboundConnectorId) {
        return "/outbound-connectors/" + outboundConnectorId;
    }
}
