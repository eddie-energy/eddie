package energy.eddie.regionconnector.de.eta;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import org.junit.jupiter.api.Test;

import java.time.Period;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EtaRegionConnectorMetadata.
 */
class EtaRegionConnectorMetadataTest {

    @Test
    void testSingletonPattern() {
        var instance1 = EtaRegionConnectorMetadata.getInstance();
        var instance2 = EtaRegionConnectorMetadata.getInstance();
        
        assertSame(instance1, instance2, "Should return same instance");
    }

    @Test
    void testBasicMetadata() {
        var metadata = EtaRegionConnectorMetadata.getInstance();
        
        assertEquals("de-eta", metadata.id());
        assertEquals("DE", metadata.countryCode());
        assertTrue(metadata.coveredMeteringPoints() > 0);
    }

    @Test
    void testTimeZone() {
        var metadata = EtaRegionConnectorMetadata.getInstance();
        
        assertEquals(ZoneId.of("Europe/Berlin"), metadata.timeZone());
        assertEquals("Europe/Berlin", EtaRegionConnectorMetadata.DE_ZONE_ID.getId());
    }

    @Test
    void testDataPeriods() {
        var metadata = EtaRegionConnectorMetadata.getInstance();
        
        assertEquals(Period.ofMonths(-36), metadata.earliestStart());
        assertEquals(Period.ofMonths(36), metadata.latestEnd());
    }

    @Test
    void testSupportedGranularities() {
        var metadata = EtaRegionConnectorMetadata.getInstance();
        var granularities = metadata.supportedGranularities();
        
        assertFalse(granularities.isEmpty());
        assertTrue(granularities.contains(Granularity.PT15M), "Should support 15-minute intervals");
        assertTrue(granularities.contains(Granularity.PT1H), "Should support hourly intervals");
        assertTrue(granularities.contains(Granularity.P1D), "Should support daily intervals");
    }

    @Test
    void testSupportedEnergyTypes() {
        var metadata = EtaRegionConnectorMetadata.getInstance();
        var energyTypes = metadata.supportedEnergyTypes();
        
        assertFalse(energyTypes.isEmpty());
        assertTrue(energyTypes.contains(EnergyType.ELECTRICITY), "Should support electricity");
        assertTrue(energyTypes.contains(EnergyType.NATURAL_GAS), "Should support natural gas");
    }

    @Test
    void testSupportedDataNeeds() {
        var metadata = EtaRegionConnectorMetadata.getInstance();
        var dataNeeds = metadata.supportedDataNeeds();
        
        assertFalse(dataNeeds.isEmpty());
        assertEquals(2, dataNeeds.size(), "Should support 2 data need types");
    }

    @Test
    void testImmutability() {
        var metadata = EtaRegionConnectorMetadata.getInstance();
        
        // Verify that returned lists are immutable or safe copies
        var granularities = metadata.supportedGranularities();
        assertThrows(UnsupportedOperationException.class, () -> {
            granularities.add(Granularity.PT5M);
        }, "Granularities list should be immutable");
    }
}
