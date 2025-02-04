package energy.eddie.regionconnector.cds.client;

import java.util.List;

public class Scopes {
    public static final String CUSTOMER_DATA_SCOPE = "customer_data";
    public static final String CLIENT_ADMIN_SCOPE = "client_admin";
    public static final List<String> REQUIRED_SCOPES = List.of(CUSTOMER_DATA_SCOPE, CLIENT_ADMIN_SCOPE);

    private Scopes() {
        // Utility class
    }
}
