package energy.eddie.regionconnector.at.eda.config;

import java.util.Properties;

import static java.util.Objects.requireNonNull;

public class PropertiesAtConfiguration implements AtConfiguration {
    private final Properties properties;

    private PropertiesAtConfiguration(Properties properties) {
        this.properties = requireNonNull(properties);
    }

    public static PropertiesAtConfiguration fromProperties(Properties properties) {
        requireNonNull(properties.getProperty(ELIGIBLE_PARTY_ID_KEY), "Property %s is required".formatted(ELIGIBLE_PARTY_ID_KEY));


        return new PropertiesAtConfiguration(properties);
    }

    @Override
    public String eligiblePartyId() {
        return requireNonNull(properties.getProperty(PropertiesAtConfiguration.ELIGIBLE_PARTY_ID_KEY), "Property %s is required".formatted(PropertiesAtConfiguration.ELIGIBLE_PARTY_ID_KEY));
    }
}
