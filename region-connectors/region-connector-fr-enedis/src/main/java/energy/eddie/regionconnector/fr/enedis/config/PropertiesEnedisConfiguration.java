package energy.eddie.regionconnector.fr.enedis.config;

import java.util.Properties;

import static java.util.Objects.requireNonNull;

public class PropertiesEnedisConfiguration implements EnedisConfiguration {

    private static final String PREFIX = "regionconnectors.fr.enedis.";
    public static final String ENEDIS_CLIENT_SECRET_KEY = PREFIX + "clientSecret";
    public static final String ENEDIS_CLIENT_ID_KEY = PREFIX + "clientId";
    private final Properties properties;

    public PropertiesEnedisConfiguration(Properties properties) {
        this.properties = requireNonNull(properties);
    }

    @Override
    public String clientId() {
        return get(ENEDIS_CLIENT_ID_KEY);
    }

    @Override
    public String clientSecret() {
        return get(ENEDIS_CLIENT_SECRET_KEY);
    }

    private String get(String key) {
        return requireNonNull(properties.getProperty(key), "Property %s is required".formatted(key));
    }
}
