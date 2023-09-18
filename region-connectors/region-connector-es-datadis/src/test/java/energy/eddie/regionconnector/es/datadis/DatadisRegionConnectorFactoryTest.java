package energy.eddie.regionconnector.es.datadis;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class DatadisRegionConnectorFactoryTest {

    @Test
    void create_withNullConfig_throwsException() {
        DatadisRegionConnectorFactory uut = new DatadisRegionConnectorFactory();

        assertThrows(NullPointerException.class, () -> uut.create(null));
    }

    @Test
    void create_withValidConfig_returnsRegionConnector() {
        var config = mock(Config.class);

        DatadisRegionConnectorFactory uut = new DatadisRegionConnectorFactory();
        var connector = uut.create(config);

        assertNotNull(connector);
    }
}