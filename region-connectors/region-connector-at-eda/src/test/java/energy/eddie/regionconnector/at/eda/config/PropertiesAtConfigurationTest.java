package energy.eddie.regionconnector.at.eda.config;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class PropertiesAtConfigurationTest {

    @Test
    void fromProperties_withValidInput_setsExpectedProperties() {
        Properties properties = new Properties();
        var expectedEligiblePartyId = "12345";
        properties.setProperty(PropertiesAtConfiguration.ELIGIBLE_PARTY_ID_KEY, expectedEligiblePartyId);

        PropertiesAtConfiguration config = PropertiesAtConfiguration.fromProperties(properties);

        assertNotNull(config);
        assertEquals(expectedEligiblePartyId, config.eligiblePartyId());
    }

    @Test
    void fromProperties_withMissingEligiblePartyId_throwsNullPointerException() {
        Properties properties = new Properties();

        assertThrows(NullPointerException.class, () -> PropertiesAtConfiguration.fromProperties(properties));
    }

    @Test
    void eligiblePartyId_whenPropertyIsRemoved_throwNullPointerException() {
        Properties properties = new Properties();
        properties.setProperty(PropertiesAtConfiguration.ELIGIBLE_PARTY_ID_KEY, "xxx");

        var config = PropertiesAtConfiguration.fromProperties(properties);

        properties.remove(PropertiesAtConfiguration.ELIGIBLE_PARTY_ID_KEY);
        assertThrows(NullPointerException.class, config::eligiblePartyId);
    }

    @Test
    void eligiblePartyId_whenPropertyIsChanged_returnNewValue() {
        Properties properties = new Properties();
        var beforeEligiblePartyId = "12345";
        properties.setProperty(PropertiesAtConfiguration.ELIGIBLE_PARTY_ID_KEY, beforeEligiblePartyId);

        var config = PropertiesAtConfiguration.fromProperties(properties);
        assertEquals(beforeEligiblePartyId, config.eligiblePartyId());

        var afterEligiblePartyId = "54321";

        properties.setProperty(PropertiesAtConfiguration.ELIGIBLE_PARTY_ID_KEY, afterEligiblePartyId);
        assertEquals(afterEligiblePartyId, config.eligiblePartyId());
    }
}