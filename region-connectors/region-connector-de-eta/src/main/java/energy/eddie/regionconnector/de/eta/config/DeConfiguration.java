package energy.eddie.regionconnector.de.eta.config;

/**
 * Configuration interface for the German (DE) ETA Plus region connector.
 * This interface defines configuration keys for the region connector.
 */
public interface DeConfiguration {
    String PREFIX = "region-connector.de.eta.";

    /**
     * Configuration key for the eligible party ID.
     * This ID must be registered with ETA Plus.
     */
    String ELIGIBLE_PARTY_ID_KEY = PREFIX + "eligibleparty.id";

    /**
     * Configuration key for the ETA Plus API base URL
     */
    String API_BASE_URL_KEY = PREFIX + "api.base-url";

    /**
     * Configuration key for the API client ID
     */
    String API_CLIENT_ID_KEY = PREFIX + "api.client-id";

    /**
     * Configuration key for the API client secret
     */
    String API_CLIENT_SECRET_KEY = PREFIX + "api.client-secret";

    /**
     * Get the eligible party ID that will be used as the sender for all messages
     * sent to ETA Plus. This ID must be registered with ETA Plus.
     * 
     * @return the eligible party ID
     */
    String eligiblePartyId();

    /**
     * Get the base URL for the ETA Plus API
     * 
     * @return the API base URL
     */
    String apiBaseUrl();

    /**
     * Get the API client ID for authentication
     * 
     * @return the API client ID
     */
    String apiClientId();

    /**
     * Get the API client secret for authentication
     * 
     * @return the API client secret
     */
    String apiClientSecret();
}
