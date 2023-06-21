package energy.eddie.regionconnector.at.eda.config;

import java.time.ZoneId;
import java.util.Properties;

import static java.util.Objects.requireNonNull;

public class PropertiesAtConfiguration implements AtConfiguration {
    private static final String PREFIX = "region-connector.at.eda.";

    private static final String ELIGIBLE_PARTY_ID = PREFIX + "eligiblePartyId";
    private static final String TIME_ZONE = PREFIX + "timeZone";
    private final Properties properties;

    private PropertiesAtConfiguration(Properties properties) {
        this.properties = requireNonNull(properties);
    }

    public static PropertiesAtConfiguration fromProperties(Properties properties) {
        requireNonNull(properties.getProperty(ELIGIBLE_PARTY_ID), "Property %s is required".formatted(ELIGIBLE_PARTY_ID));

        ZoneId zoneId = ZoneId.of(requireNonNull(properties.getProperty(TIME_ZONE), "Property %s is required".formatted(TIME_ZONE)));
        ;

        return new PropertiesAtConfiguration(properties);
    }

    @Override
    public String eligiblePartyId() {
        return get(ELIGIBLE_PARTY_ID);
    }

    @Override
    public ZoneId timeZone() {
        return ZoneId.of(get(TIME_ZONE));
    }

    private String get(String key) {
        return requireNonNull(properties.getProperty(key), "Property %s is required".formatted(key));
    }
}
