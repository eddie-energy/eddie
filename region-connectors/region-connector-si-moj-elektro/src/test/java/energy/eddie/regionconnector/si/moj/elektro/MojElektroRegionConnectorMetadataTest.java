package energy.eddie.regionconnector.si.moj.elektro;

import org.junit.jupiter.api.Test;

import java.time.Period;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MojElektroRegionConnectorMetadataTest {

    private final MojElektroRegionConnectorMetadata metadata = new MojElektroRegionConnectorMetadata();

    @Test
    void testId() {
        assertEquals("si-moj-elektro", metadata.id());
    }

    @Test
    void testCountryCode() {
        assertEquals("SI", metadata.countryCode());
    }

    @Test
    void testCoveredMeteringPoints() {
        assertEquals(0, metadata.coveredMeteringPoints());
    }

    @Test
    void testEarliestStart() {
        assertEquals(Period.ZERO, metadata.earliestStart());
    }

    @Test
    void testLatestEnd() {
        assertEquals(Period.ZERO, metadata.latestEnd());
    }

    @Test
    void testSupportedGranularities() {
        List<?> granularities = metadata.supportedGranularities();

        assertNotNull(granularities);
        assertTrue(granularities.isEmpty());
    }

    @Test
    void testTimeZone() {
        assertEquals(ZoneOffset.UTC, metadata.timeZone());
    }

    @Test
    void testSupportedEnergyTypes() {
        List<?> energyTypes = metadata.supportedEnergyTypes();

        assertNotNull(energyTypes);
        assertTrue(energyTypes.isEmpty());
    }

    @Test
    void testSupportedDataNeeds() {
        List<?> dataNeeds = metadata.supportedDataNeeds();

        assertNotNull(dataNeeds);
        assertTrue(dataNeeds.isEmpty());
    }
}
