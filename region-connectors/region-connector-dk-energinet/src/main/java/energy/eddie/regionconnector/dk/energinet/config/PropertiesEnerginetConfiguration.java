package energy.eddie.regionconnector.dk.energinet.config;

import java.util.Properties;

import static java.util.Objects.requireNonNull;

public class PropertiesEnerginetConfiguration implements EnerginetConfiguration {

    private final Properties properties;

    public PropertiesEnerginetConfiguration(Properties properties) {
        this.properties = requireNonNull(properties);

        customerBasePath();
        thirdpartyBasePath();
    }

    private String get(String key) {
        return requireNonNull(properties.getProperty(key), "Property %s is required".formatted(key));
    }

    @Override
    public String customerBasePath() {
        return get(ENERGINET_CUSTOMER_BASE_PATH_KEY);
    }

    @Override
    public String thirdpartyBasePath() {
        return get(ENERGINET_THIRDPARTY_BASE_PATH_KEY);
    }
}
