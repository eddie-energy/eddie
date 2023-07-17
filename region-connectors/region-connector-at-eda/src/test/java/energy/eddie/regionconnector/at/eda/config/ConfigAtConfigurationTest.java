package energy.eddie.regionconnector.at.eda.config;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfigAtConfigurationTest {


    @Test
    void configurationThrows_ifConfigNull() {
        assertThrows(NullPointerException.class, () -> new ConfigAtConfiguration(null));
    }

    @Test
    void configurationConstructs() {
        var config = mock(Config.class);

        var uut = new ConfigAtConfiguration(config);

        assertNotNull(uut);
    }

    @Test
    void eligiblePartyId_withValidConfig_returnsExpected() {
        var config = mock(Config.class);
        var expectedEligiblePartyId = "12345";
        when(config.getValue(AtConfiguration.ELIGIBLE_PARTY_ID_KEY, String.class)).thenReturn(expectedEligiblePartyId);


        var uut = new ConfigAtConfiguration(config);

        var actualEligiblePartyId = uut.eligiblePartyId();

        assertEquals(expectedEligiblePartyId, actualEligiblePartyId);
    }

    @Test
    void eligiblePartyId_withMissingValue_throwsNoSuchElementException() {
        var config = mock(Config.class);
        when(config.getValue(AtConfiguration.ELIGIBLE_PARTY_ID_KEY, String.class)).thenThrow(NoSuchElementException.class);

        var uut = new ConfigAtConfiguration(config);
        assertThrows(NoSuchElementException.class, uut::eligiblePartyId);
    }

    @Test
    void eligiblePartyId_withValidConfig_returnsUpdated() {
        var config = mock(Config.class);
        var expectedEligiblePartyId = "12345";
        var updatedEligiblePartyId = "54321";
        when(config.getValue(AtConfiguration.ELIGIBLE_PARTY_ID_KEY, String.class))
                .thenReturn(expectedEligiblePartyId)
                .thenReturn(updatedEligiblePartyId);


        var uut = new ConfigAtConfiguration(config);

        var firstEligiblePartyId = uut.eligiblePartyId();

        assertEquals(expectedEligiblePartyId, firstEligiblePartyId);

        var secondEligiblePartyId = uut.eligiblePartyId();

        assertEquals(updatedEligiblePartyId, secondEligiblePartyId);
    }

}