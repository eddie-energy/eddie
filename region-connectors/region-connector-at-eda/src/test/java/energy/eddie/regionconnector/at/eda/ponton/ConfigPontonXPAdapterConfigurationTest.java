package energy.eddie.regionconnector.at.eda.ponton;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfigPontonXPAdapterConfigurationTest {
    @Test
    void configurationThrows_ifConfigNull() {
        assertThrows(NullPointerException.class, () -> new ConfigPontonXPAdapterConfiguration(null));
    }

    @Test
    void configurationConstructs() {
        var config = mock(Config.class);

        var uut = new ConfigPontonXPAdapterConfiguration(config);

        assertNotNull(uut);
    }

    @Test
    void getters_withValidConfig_returnExpected() {
        var config = mock(Config.class);
        var expectedAdapterId = "12345";
        var expectedAdapterVersion = "adapterVersion";
        var expectedHostname = "hostname";
        var expectedPort = 1234;
        var expectedWorkFolder = "workFolder";

        when(config.getValue(ConfigPontonXPAdapterConfiguration.ADAPTER_ID_KEY, String.class)).thenReturn(expectedAdapterId);
        when(config.getValue(ConfigPontonXPAdapterConfiguration.ADAPTER_VERSION_KEY, String.class)).thenReturn(expectedAdapterVersion);
        when(config.getValue(ConfigPontonXPAdapterConfiguration.HOSTNAME_KEY, String.class)).thenReturn(expectedHostname);
        when(config.getOptionalValue(ConfigPontonXPAdapterConfiguration.PORT_KEY, Integer.class)).thenReturn(Optional.of(expectedPort));
        when(config.getValue(ConfigPontonXPAdapterConfiguration.WORK_FOLDER_KEY, String.class)).thenReturn(expectedWorkFolder);

        var uut = new ConfigPontonXPAdapterConfiguration(config);

        assertEquals(expectedAdapterId, uut.adapterId());
        assertEquals(expectedAdapterVersion, uut.adapterVersion());
        assertEquals(expectedHostname, uut.hostname());
        assertEquals(expectedPort, uut.port());
        assertEquals(expectedWorkFolder, uut.workFolder());
    }

    @Test
    void getter_withMissingValues_throwsNoSuchElementException() {
        var config = mock(Config.class);
        when(config.getValue(ConfigPontonXPAdapterConfiguration.ADAPTER_ID_KEY, String.class)).thenReturn("id").thenThrow(NoSuchElementException.class);
        when(config.getValue(ConfigPontonXPAdapterConfiguration.ADAPTER_VERSION_KEY, String.class)).thenReturn("version").thenThrow(NoSuchElementException.class);
        when(config.getValue(ConfigPontonXPAdapterConfiguration.HOSTNAME_KEY, String.class)).thenReturn("host").thenThrow(NoSuchElementException.class);
        when(config.getValue(ConfigPontonXPAdapterConfiguration.WORK_FOLDER_KEY, String.class)).thenReturn("folder").thenThrow(NoSuchElementException.class);

        var uut = new ConfigPontonXPAdapterConfiguration(config);
        assertThrows(NoSuchElementException.class, uut::adapterId);
        assertThrows(NoSuchElementException.class, uut::adapterVersion);
        assertThrows(NoSuchElementException.class, uut::hostname);
        assertThrows(NoSuchElementException.class, uut::workFolder);
    }

    @Test
    void getters_withValidConfig_returnsUpdated() {
        var config = mock(Config.class);
        var expectedAdapterId = "12345";
        var updatedAdapterId = "54321";
        var expectedAdapterVersion = "adapterVersion";
        var updatedAdapterVersion = "updatedAdapterVersion";
        var expectedHostname = "hostname";
        var updatedHostname = "updatedHostname";
        var expectedPort = 1234;
        var updatedPort = 4321;
        var expectedWorkFolder = "workFolder";
        var updatedWorkFolder = "updatedWorkFolder";

        when(config.getValue(ConfigPontonXPAdapterConfiguration.ADAPTER_ID_KEY, String.class))
                .thenReturn(expectedAdapterId)
                .thenReturn(expectedAdapterId)
                .thenReturn(updatedAdapterId);
        when(config.getValue(ConfigPontonXPAdapterConfiguration.ADAPTER_VERSION_KEY, String.class))
                .thenReturn(expectedAdapterVersion)
                .thenReturn(expectedAdapterVersion)
                .thenReturn(updatedAdapterVersion);
        when(config.getValue(ConfigPontonXPAdapterConfiguration.HOSTNAME_KEY, String.class))
                .thenReturn(expectedHostname)
                .thenReturn(expectedHostname)
                .thenReturn(updatedHostname);
        when(config.getOptionalValue(ConfigPontonXPAdapterConfiguration.PORT_KEY, Integer.class))
                .thenReturn(Optional.of(expectedPort))
                .thenReturn(Optional.of(updatedPort));
        when(config.getValue(ConfigPontonXPAdapterConfiguration.WORK_FOLDER_KEY, String.class))
                .thenReturn(expectedWorkFolder)
                .thenReturn(expectedWorkFolder)
                .thenReturn(updatedWorkFolder);


        var uut = new ConfigPontonXPAdapterConfiguration(config);

        // first values
        assertAll(
                () -> assertEquals(expectedAdapterId, uut.adapterId()),
                () -> assertEquals(expectedAdapterVersion, uut.adapterVersion()),
                () -> assertEquals(expectedHostname, uut.hostname()),
                () -> assertEquals(expectedPort, uut.port()),
                () -> assertEquals(expectedWorkFolder, uut.workFolder())
        );

        // updated values
        assertAll(
                () -> assertEquals(updatedAdapterId, uut.adapterId()),
                () -> assertEquals(updatedAdapterVersion, uut.adapterVersion()),
                () -> assertEquals(updatedHostname, uut.hostname()),
                () -> assertEquals(updatedPort, uut.port()),
                () -> assertEquals(updatedWorkFolder, uut.workFolder())
        );
    }

    @Test
    void port_withMissingValue_returnsDefault() {
        var config = mock(Config.class);
        when(config.getOptionalValue(ConfigPontonXPAdapterConfiguration.PORT_KEY, Integer.class)).thenReturn(Optional.empty());

        var uut = new ConfigPontonXPAdapterConfiguration(config);

        var actualPort = uut.port();

        assertEquals(ConfigPontonXPAdapterConfiguration.DEFAULT_PORT, actualPort);
    }
}