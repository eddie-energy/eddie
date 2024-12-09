package energy.eddie.outbound.admin.console.config;

public interface AdminConsoleConfig {
    String PREFIX = "outbound-connector.admin-console.";

    // Admin Username and Password are not loaded in the traditional way (as BeanConfig), because Configuration Beans are instantiated after the Security Config.
    String LOGIN_ENABLED = PREFIX + "login.enabled";
    String LOGIN_USERNAME = PREFIX + "login.username";
    String LOGIN_ENCODED_PASSWORD = PREFIX + "login.encodedPassword";
}
