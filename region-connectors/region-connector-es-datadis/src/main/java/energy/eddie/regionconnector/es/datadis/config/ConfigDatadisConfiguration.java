package energy.eddie.regionconnector.es.datadis.config;

import org.eclipse.microprofile.config.Config;

import static java.util.Objects.requireNonNull;

public class ConfigDatadisConfiguration implements DatadisConfig {
    private final Config config;

    public ConfigDatadisConfiguration(Config config) {
        requireNonNull(config);
        this.config = config;
    }


    @Override
    public String username() {
        return config.getValue(USERNAME_KEY, String.class);
    }

    @Override
    public String password() {
        return config.getValue(PASSWORD_KEY, String.class);
    }
}
