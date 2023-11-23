package energy.eddie.regionconnector.at.eda;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class EdaRegionConnectorFactoryTest {

    @Test
    void create_withValidConfig_returnsRegionConnector() throws Exception {
        var config = mock(Config.class);

        EdaRegionConnectorFactory uut = new EdaRegionConnectorFactory();
        var connector = uut.create(config);

        assertNotNull(connector);
    }

}