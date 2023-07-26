package energy.eddie.regionconnector.at.eda;

import energy.eddie.api.v0.RegionConnectorProvisioningException;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapterConfiguration;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EdaRegionConnectorFactoryTest {

    @Test
    void create_withNullConfig_throwsException() {
        EdaRegionConnectorFactory uut = new EdaRegionConnectorFactory();

        assertThrows(NullPointerException.class, () -> uut.create(null));
    }

    @Test
    void create_withInvalidAtConfig_throwsRegionConnectorProvisioningException() {
        EdaRegionConnectorFactory uut = new EdaRegionConnectorFactory();

        var config = mock(Config.class);
        when(config.getValue(AtConfiguration.ELIGIBLE_PARTY_ID_KEY, String.class)).thenThrow(NoSuchElementException.class);

        assertThrows(RegionConnectorProvisioningException.class, () -> uut.create(config));
    }

    @Test
    void create_withInvalidPontonXPAdapterConfig_throwsRegionConnectorProvisioningException() {
        EdaRegionConnectorFactory uut = new EdaRegionConnectorFactory();

        var config = mock(Config.class);
        when(config.getValue(AtConfiguration.ELIGIBLE_PARTY_ID_KEY, String.class)).thenReturn("id");

        when(config.getValue(PontonXPAdapterConfiguration.ADAPTER_ID_KEY, String.class)).thenThrow(NoSuchElementException.class);
        when(config.getValue(PontonXPAdapterConfiguration.ADAPTER_VERSION_KEY, String.class)).thenThrow(NoSuchElementException.class);
        when(config.getValue(PontonXPAdapterConfiguration.HOSTNAME_KEY, String.class)).thenThrow(NoSuchElementException.class);
        when(config.getValue(PontonXPAdapterConfiguration.WORK_FOLDER_KEY, String.class)).thenThrow(NoSuchElementException.class);

        assertThrows(RegionConnectorProvisioningException.class, () -> uut.create(config));
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