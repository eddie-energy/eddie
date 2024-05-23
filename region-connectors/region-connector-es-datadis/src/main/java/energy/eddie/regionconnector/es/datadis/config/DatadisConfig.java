package energy.eddie.regionconnector.es.datadis.config;

public interface DatadisConfig {

    String PREFIX = "region-connector.es.datadis.";

    String USERNAME_KEY = PREFIX + "username";

    String PASSWORD_KEY = PREFIX + "password";

    String BASE_PATH_KEY = PREFIX + "basepath";

    String username();

    String password();

    String basePath();
}