package energy.eddie.regionconnector.es.datadis.client;

import java.net.URI;

/**
 * Provides the endpoints for the Datadis API.
 */
public class DatadisEndpoints {

    private static final String BASE_URL = "https://datadis.es/";
    private static final URI TOKEN_ENDPOINT = URI.create(BASE_URL + "nikola-auth/tokens/login");

    private static final String PRIVATE_APIS_BASE_URL = BASE_URL + "api-private/api/";

    private static final URI SUPPLIES_ENDPOINT = URI.create(PRIVATE_APIS_BASE_URL + "get-supplies");


    private static final URI CONSUMPTION_KWH_ENDPOINT = URI.create(PRIVATE_APIS_BASE_URL + "get-consumption-data");

    private static final URI AUTHORIZATION_REQUEST_ENDPOINT = URI.create(BASE_URL + "api-private/request/send-request-authorization");

    public URI tokenEndpoint() {
        return TOKEN_ENDPOINT;
    }

    public URI suppliesEndpoint() {
        return SUPPLIES_ENDPOINT;
    }

    public URI consumptionKwhEndpoint() {
        return CONSUMPTION_KWH_ENDPOINT;
    }

    public URI authorizationRequestEndpoint() {
        return AUTHORIZATION_REQUEST_ENDPOINT;
    }
}
