package energy.eddie.regionconnector.dk.energinet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class EnerginetRegionConnectorMetadataTest {

    @Test
    void getInstance() {
        var instance1 = EnerginetRegionConnectorMetadata.getInstance();
        var instance2 = EnerginetRegionConnectorMetadata.getInstance();

        assertSame(instance1, instance2);
    }

    @Test
    void id() {
        assertEquals("dk-energinet", EnerginetRegionConnectorMetadata.getInstance().id());
    }

    @Test
    void countryCode() {
        assertEquals("DK", EnerginetRegionConnectorMetadata.getInstance().countryCode());
    }

    @Test
    void coveredMeteringPoints() {
        assertEquals(3300000, EnerginetRegionConnectorMetadata.getInstance().coveredMeteringPoints());
    }
}