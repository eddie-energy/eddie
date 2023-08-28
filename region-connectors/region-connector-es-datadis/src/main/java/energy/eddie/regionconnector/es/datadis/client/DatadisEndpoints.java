package energy.eddie.regionconnector.es.datadis.client;

import java.net.URI;

/**
 * Constants for Datadis API endpoints.
 */
public class DatadisEndpoints {

    public static final String BASE_URL = "https://datadis.es/";
    public static final URI AUTH_ENDPOINT = URI.create(BASE_URL + "nikola-auth/tokens/login");

    public static final String PRIVATE_APIS_BASE_URL = BASE_URL + "api-private/api/";

    public static final URI SUPPLIES_ENDPOINT = URI.create(PRIVATE_APIS_BASE_URL + "get-supplies");


    public static final URI CONSUMPTION_KWH_ENDPOINT = URI.create(PRIVATE_APIS_BASE_URL + "get-consumption-data");

    public static final URI AUTHORIZATION_REQUEST_ENDPOINT = URI.create(BASE_URL + "api-private/request/send-request-authorization");

    private DatadisEndpoints() {
    }
}
