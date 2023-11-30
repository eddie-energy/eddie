package energy.eddie.regionconnector.at.eda;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class EdaRegionConnectorMetadataTest {

    @Test
    void getInstance_returnsSameInstance() {
        var instance1 = EdaRegionConnectorMetadata.getInstance();
        var instance2 = EdaRegionConnectorMetadata.getInstance();

        assertSame(instance1, instance2);
    }

    @Test
    void id() {
        assertEquals("at-eda", EdaRegionConnectorMetadata.getInstance().id());
    }

    @Test
    void countryCode() {
        assertEquals("at", EdaRegionConnectorMetadata.getInstance().countryCode());
    }

    @Test
    void coveredMeteringPoints() {
        assertEquals(5977915, EdaRegionConnectorMetadata.getInstance().coveredMeteringPoints());
    }
}