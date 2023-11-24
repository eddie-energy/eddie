package energy.eddie.regionconnector.dk.energinet.config;


public interface EnerginetConfiguration {
    String PREFIX = "region-connector.dk.energinet.";
    String ENERGINET_CUSTOMER_BASE_PATH_KEY = PREFIX + "customer.client.basePath";
    String ENERGINET_THIRD_PARTY_BASE_PATH_KEY = PREFIX + "thirdparty.client.basePath";

    /**
     * BasePath for the customer api
     */
    String customerBasePath();

    /**
     * BasePath for the thirdparty api
     */
    String thirdPartyBasePath();
}
