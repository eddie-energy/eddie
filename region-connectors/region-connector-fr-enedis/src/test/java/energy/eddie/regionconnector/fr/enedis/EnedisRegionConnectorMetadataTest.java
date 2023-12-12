package energy.eddie.regionconnector.fr.enedis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class EnedisRegionConnectorMetadataTest {

    @Test
    void getInstance() {
        var instance1 = EnedisRegionConnectorMetadata.getInstance();
        var instance2 = EnedisRegionConnectorMetadata.getInstance();

        assertSame(instance1, instance2);
    }

    @Test
    void id() {
        assertEquals("fr-enedis", EnedisRegionConnectorMetadata.getInstance().id());
    }

    @Test
    void countryCode() {
        assertEquals("FR", EnedisRegionConnectorMetadata.getInstance().countryCode());
    }

    @Test
    void coveredMeteringPoints() {
        assertEquals(36951446, EnedisRegionConnectorMetadata.getInstance().coveredMeteringPoints());
    }
}