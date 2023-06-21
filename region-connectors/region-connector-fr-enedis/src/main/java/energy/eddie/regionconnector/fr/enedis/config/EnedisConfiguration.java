package energy.eddie.regionconnector.fr.enedis.config;


public interface EnedisConfiguration {

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
