package energy.eddie.regionconnector.fr.enedis.config;

import java.util.Properties;

import static java.util.Objects.requireNonNull;

public class PropertiesEnedisConfiguration implements EnedisConfiguration {

    private final Properties properties;

    public PropertiesEnedisConfiguration(Properties properties) {
        this.properties = requireNonNull(properties);

        var clientId = properties.getProperty(ENEDIS_CLIENT_ID_KEY);
        requireNonNull(clientId, "Property %s is required".formatted(ENEDIS_CLIENT_ID_KEY));
        var clientSecret = properties.getProperty(ENEDIS_CLIENT_SECRET_KEY);
        requireNonNull(clientSecret, "Property %s is required".formatted(ENEDIS_CLIENT_SECRET_KEY));
    }

    @Override
    public String clientId() {
        return get(ENEDIS_CLIENT_ID_KEY);
    }

    @Override
    public String clientSecret() {
        return get(ENEDIS_CLIENT_SECRET_KEY);
    }

    @Override
    public String basePath() {
        return properties.getProperty(ENEDIS_BASE_PATH_ID_KEY, ENEDIS_DEFAULT_BASE_PATH);
    }

    private String get(String key) {
        return requireNonNull(properties.getProperty(key), "Property %s is required".formatted(key));
    }
}
