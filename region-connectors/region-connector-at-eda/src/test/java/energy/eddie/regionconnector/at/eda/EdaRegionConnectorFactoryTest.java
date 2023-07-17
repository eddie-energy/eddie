package energy.eddie.regionconnector.at.eda;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class EdaRegionConnectorFactoryTest {

    @Test
    void create_withNullConfig_throwsException() {
        EdaRegionConnectorFactory uut = new EdaRegionConnectorFactory();

        assertThrows(NullPointerException.class, () -> uut.create(null));
    }

    @Test
    @Disabled("This test can't be run, as the created EdaAdapter needs a working connection to a PontonXPMessenger and a folder existing on the system.")
    void create_withValidConfig_returnsRegionConnector() throws Exception {
        var config = mock(Config.class);

        EdaRegionConnectorFactory uut = new EdaRegionConnectorFactory();
        var connector = uut.create(config);

        assertNotNull(connector);
    }

}