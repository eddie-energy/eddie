package energy.eddie.regionconnector.cds.client;

import java.util.List;

public class Scopes {
    // Automated client creation
    public static final String CUSTOMER_DATA_SCOPE = "customer_data";
    public static final String CLIENT_ADMIN_SCOPE = "client_admin";
    public static final List<String> REQUIRED_SCOPES = List.of(CUSTOMER_DATA_SCOPE, CLIENT_ADMIN_SCOPE);

    // User scopes
    public static final String USAGE_DETAILED_SCOPE = "cds_usage_detailed";

    private Scopes() {
        // Utility class
    }
}
