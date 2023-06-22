package energy.eddie.regionconnector.at.eda.config;

import org.junit.jupiter.api.Test;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.zone.ZoneRulesException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class PropertiesAtConfigurationTest {

    @Test
    void fromProperties_WithValidInput_SetsExpectedProperties() {
        Properties properties = new Properties();
        var expectedEligiblePartyId = "12345";
        var expectedTimeZone = ZoneId.of("Europe/Vienna");
        properties.setProperty(PropertiesAtConfiguration.ELIGIBLE_PARTY_ID_KEY, expectedEligiblePartyId);
        properties.setProperty(PropertiesAtConfiguration.TIME_ZONE_KEY, expectedTimeZone.toString());

        PropertiesAtConfiguration config = PropertiesAtConfiguration.fromProperties(properties);

        assertNotNull(config);
        assertEquals(expectedEligiblePartyId, config.eligiblePartyId());
        assertEquals(expectedTimeZone, config.timeZone());
    }

    @Test
    void fromProperties_WithMissingEligiblePartyId_ThrowsNullPointerException() {
        Properties properties = new Properties();
        properties.setProperty(PropertiesAtConfiguration.TIME_ZONE_KEY, "Europe/Vienna");

        assertThrows(NullPointerException.class, () -> PropertiesAtConfiguration.fromProperties(properties));
    }

    @Test
    void fromProperties_WithMissingTimeZone_ThrowsNullPointerException() {
        Properties properties = new Properties();
        properties.setProperty(PropertiesAtConfiguration.ELIGIBLE_PARTY_ID_KEY, "12345");

        assertThrows(NullPointerException.class, () -> PropertiesAtConfiguration.fromProperties(properties));
    }

    @Test
    void fromProperties_WithInvalidTimeZone_ThrowsZoneRulesException() {
        Properties properties = new Properties();
        properties.setProperty(PropertiesAtConfiguration.ELIGIBLE_PARTY_ID_KEY, "12345");
        properties.setProperty(PropertiesAtConfiguration.TIME_ZONE_KEY, "Europe/Invalid");

        assertThrows(ZoneRulesException.class, () -> PropertiesAtConfiguration.fromProperties(properties));
    }

    @Test
    void fromProperties_WithWronglyFormattedTimeZone_ThrowsDateTimeException() {
        Properties properties = new Properties();
        properties.setProperty(PropertiesAtConfiguration.ELIGIBLE_PARTY_ID_KEY, "12345");
        properties.setProperty(PropertiesAtConfiguration.TIME_ZONE_KEY, "Europe/Vienna/Invalid");

        assertThrows(DateTimeException.class, () -> PropertiesAtConfiguration.fromProperties(properties));
    }

    @Test
    void getters_WhenPropertiesAreRemoved_ThrowNullPointerException() {
        Properties properties = new Properties();
        properties.setProperty(PropertiesAtConfiguration.ELIGIBLE_PARTY_ID_KEY, "xxx");
        properties.setProperty(PropertiesAtConfiguration.TIME_ZONE_KEY, "Europe/Vienna");

        var config = PropertiesAtConfiguration.fromProperties(properties);

        properties.remove(PropertiesAtConfiguration.ELIGIBLE_PARTY_ID_KEY);
        assertThrows(NullPointerException.class, config::eligiblePartyId);
        properties.remove(PropertiesAtConfiguration.TIME_ZONE_KEY);
        assertThrows(NullPointerException.class, config::timeZone);
    }

    @Test
    void getters_WhenPropertiesAreChanged_ReturnNewValue() {
        Properties properties = new Properties();
        var beforeEligiblePartyId = "12345";
        var beforeTimeZone = ZoneId.of("Europe/Vienna");
        properties.setProperty(PropertiesAtConfiguration.ELIGIBLE_PARTY_ID_KEY, beforeEligiblePartyId);
        properties.setProperty(PropertiesAtConfiguration.TIME_ZONE_KEY, beforeTimeZone.toString());

        var config = PropertiesAtConfiguration.fromProperties(properties);
        assertEquals(beforeEligiblePartyId, config.eligiblePartyId());
        assertEquals(beforeTimeZone, config.timeZone());

        var afterEligiblePartyId = "54321";
        var afterTimeZone = ZoneId.of("Europe/Berlin");

        properties.setProperty(PropertiesAtConfiguration.ELIGIBLE_PARTY_ID_KEY, afterEligiblePartyId);
        properties.setProperty(PropertiesAtConfiguration.TIME_ZONE_KEY, afterTimeZone.toString());
        assertEquals(afterEligiblePartyId, config.eligiblePartyId());
        assertEquals(afterTimeZone, config.timeZone());
    }
}