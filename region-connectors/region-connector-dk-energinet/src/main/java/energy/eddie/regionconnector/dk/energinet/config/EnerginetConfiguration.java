package energy.eddie.regionconnector.dk.energinet.config;


public interface EnerginetConfiguration {
    String PREFIX = "region-connector.dk.energinet.";
    String ENERGINET_CUSTOMER_BASE_PATH_KEY = PREFIX + "customer.client.basePath";

    /**
     * BasePath for the customer api
     */
    String customerBasePath();
}
