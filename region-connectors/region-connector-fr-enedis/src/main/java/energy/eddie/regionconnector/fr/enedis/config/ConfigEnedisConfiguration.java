package energy.eddie.regionconnector.fr.enedis.config;

import org.eclipse.microprofile.config.Config;

import static java.util.Objects.requireNonNull;

public class ConfigEnedisConfiguration implements EnedisConfiguration {
    private final Config config;

    public ConfigEnedisConfiguration(Config config) {
        requireNonNull(config);
        this.config = config;
    }

    @Override
    public String clientId() {
        return config.getValue(ENEDIS_CLIENT_ID_KEY, String.class);
    }

    @Override
    public String clientSecret() {
        return config.getValue(ENEDIS_CLIENT_SECRET_KEY, String.class);
    }

    @Override
    public String basePath() {
        return config.getValue(ENEDIS_BASE_PATH_KEY, String.class);
    }
}
