package energy.eddie.regionconnector.es.datadis.config;

public interface DatadisConfig {

    String PREFIX = "region-connector.es.datadis.";

    String USERNAME_KEY = PREFIX + "username";

    String PASSWORD_KEY = PREFIX + "password";

    String username();

    String password();
}
