package energy.eddie.regionconnector.fr.enedis.config;


public interface EnedisConfiguration {

    /**
     * BasePath is optional and can be changed to sandbox environment - default is production
     */
    String ENEDIS_DEFAULT_BASE_PATH = "https://ext.prod.api.enedis.fr";
    String PREFIX = "region-connector.fr.enedis.";
    String ENEDIS_CLIENT_ID_KEY = PREFIX + "clientId";
    String ENEDIS_CLIENT_SECRET_KEY = PREFIX + "clientSecret";
    String ENEDIS_BASE_PATH_ID_KEY = PREFIX + "basePath";

    /**
     * Client ID that will be used to authenticate with Enedis. Must be from an Application registered with Enedis.
     */
    String clientId();

    /**
     * Client Secret that will be used to authenticate with Enedis. Must be from an Application registered with Enedis.
     */
    String clientSecret();

    /**
     * BasePath is optional and can be changed to the sandbox environment of Enedis for testing - default is production
     */
    String basePath();
}
