package energy.eddie.regionconnector.at.eda.config;

import java.time.ZoneId;
import java.util.Properties;

import static java.util.Objects.requireNonNull;

public class PropertiesAtConfiguration implements AtConfiguration {

    private static final String ELIGIBLE_PARTY_ID = "eligiblePartyId";
    private static final String TIME_ZONE = "timeZone";
    private final String eligiblePartyId;
    private final ZoneId timeZone;

    private PropertiesAtConfiguration(String eligiblePartyId, ZoneId timeZone) {
        this.eligiblePartyId = eligiblePartyId;
        this.timeZone = timeZone;
    }

    public static PropertiesAtConfiguration fromProperties(Properties properties) {
        var eligiblePartyId = properties.getProperty(ELIGIBLE_PARTY_ID);
        requireNonNull(eligiblePartyId, "Missing property: " + ELIGIBLE_PARTY_ID);
        var timeZone = properties.getProperty(TIME_ZONE);
        requireNonNull(timeZone, "Missing property: " + TIME_ZONE);
        ZoneId zoneId = ZoneId.of(timeZone);
        return new PropertiesAtConfiguration(eligiblePartyId, zoneId);
    }

    @Override
    public String eligiblePartyId() {
        return eligiblePartyId;
    }

    @Override
    public ZoneId timeZone() {
        return timeZone;
    }
}
