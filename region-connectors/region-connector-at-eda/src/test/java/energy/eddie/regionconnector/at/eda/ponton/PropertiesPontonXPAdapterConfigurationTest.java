package energy.eddie.regionconnector.at.eda.ponton;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class PropertiesPontonXPAdapterConfigurationTest {

    @Test
    void fromProperties_withValidInput_setsExpectedProperties() {
        Properties properties = new Properties();
        var expectedAdapterId = "1";
        var expectedAdapterVersion = "1.0.0";
        var expectedHostname = "localhost";
        var expectedWorkFolder = "work";
        var expectedPort = 1234;
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.ADAPTER_ID_KEY, expectedAdapterId);
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.ADAPTER_VERSION_KEY, expectedAdapterVersion);
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.HOSTNAME_KEY, expectedHostname);
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.WORK_FOLDER_KEY, expectedWorkFolder);
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.PORT_KEY, String.valueOf(expectedPort));

        var config = PropertiesPontonXPAdapterConfiguration.fromProperties(properties);

        assertNotNull(config);
        assertEquals(expectedAdapterId, config.adapterId());
        assertEquals(expectedAdapterVersion, config.adapterVersion());
        assertEquals(expectedHostname, config.hostname());
        assertEquals(expectedWorkFolder, config.workFolder());
        assertEquals(expectedPort, config.port());
    }

    @Test
    void fromProperties_withInvalidPort_throwsIllegalArgumentException() {
        Properties properties = new Properties();
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.ADAPTER_ID_KEY, "2");
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.ADAPTER_VERSION_KEY, "1.0.0");
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.HOSTNAME_KEY, "localhost");
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.WORK_FOLDER_KEY, "work");
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.PORT_KEY, String.valueOf(-1));

        assertThrows(IllegalArgumentException.class, () -> PropertiesPontonXPAdapterConfiguration.fromProperties(properties));
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.PORT_KEY, String.valueOf(-65536));
        assertThrows(IllegalArgumentException.class, () -> PropertiesPontonXPAdapterConfiguration.fromProperties(properties));
    }

    @Test
    void fromProperties_withNoPort_returnsDefaultPort() {
        Properties properties = new Properties();
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.ADAPTER_ID_KEY, "3");
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.ADAPTER_VERSION_KEY, "1.0.0");
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.HOSTNAME_KEY, "localhost");
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.WORK_FOLDER_KEY, "work");

        var config = PropertiesPontonXPAdapterConfiguration.fromProperties(properties);

        assertEquals(PropertiesPontonXPAdapterConfiguration.DEFAULT_PORT, config.port());
    }

    @Test
    void fromProperties_withMissingAdapterId_throwsNullPointerException() {
        Properties properties = new Properties();
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.ADAPTER_VERSION_KEY, "1.0.0");
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.HOSTNAME_KEY, "localhost");
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.WORK_FOLDER_KEY, "work");

        assertThrows(NullPointerException.class, () -> PropertiesPontonXPAdapterConfiguration.fromProperties(properties));
    }

    @Test
    void fromProperties_withMissingAdapterVersion_throwsNullPointerException() {
        Properties properties = new Properties();
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.ADAPTER_ID_KEY, "5");
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.HOSTNAME_KEY, "localhost");
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.WORK_FOLDER_KEY, "work");

        assertThrows(NullPointerException.class, () -> PropertiesPontonXPAdapterConfiguration.fromProperties(properties));
    }

    @Test
    void fromProperties_withMissingHostname_throwsNullPointerException() {
        Properties properties = new Properties();
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.ADAPTER_ID_KEY, "6");
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.ADAPTER_VERSION_KEY, "1.0.0");
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.WORK_FOLDER_KEY, "work");

        assertThrows(NullPointerException.class, () -> PropertiesPontonXPAdapterConfiguration.fromProperties(properties));
    }

    @Test
    void fromProperties_withMissingWorkFolder_throwsNullPointerException() {
        Properties properties = new Properties();
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.ADAPTER_ID_KEY, "7");
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.ADAPTER_VERSION_KEY, "1.0.0");
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.HOSTNAME_KEY, "localhost");

        assertThrows(NullPointerException.class, () -> PropertiesPontonXPAdapterConfiguration.fromProperties(properties));
    }

    @Test
    void getters_whenPropertiesAreRemoved_throwNullPointerException() {
        Properties properties = new Properties();
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.ADAPTER_ID_KEY, "8");
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.ADAPTER_VERSION_KEY, "1.0.0");
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.HOSTNAME_KEY, "localhost");
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.WORK_FOLDER_KEY, "work");
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.PORT_KEY, String.valueOf(1234));

        var config = PropertiesPontonXPAdapterConfiguration.fromProperties(properties);

        properties.remove(PropertiesPontonXPAdapterConfiguration.ADAPTER_ID_KEY);
        assertThrows(NullPointerException.class, config::adapterId);
        properties.remove(PropertiesPontonXPAdapterConfiguration.ADAPTER_VERSION_KEY);
        assertThrows(NullPointerException.class, config::adapterVersion);
        properties.remove(PropertiesPontonXPAdapterConfiguration.HOSTNAME_KEY);
        assertThrows(NullPointerException.class, config::hostname);
        properties.remove(PropertiesPontonXPAdapterConfiguration.WORK_FOLDER_KEY);
        assertThrows(NullPointerException.class, config::workFolder);
    }

    @Test
    void getters_whenPropertiesAreChanged_returnNewValue() {
        Properties properties = new Properties();
        var beforeAdapterId = "10";
        var beforeAdapterVersion = "1.0.0";
        var beforeHostname = "localhost";
        var beforeWorkFolder = "work";
        var beforePort = 1234;
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.ADAPTER_ID_KEY, beforeAdapterId);
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.ADAPTER_VERSION_KEY, beforeAdapterVersion);
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.HOSTNAME_KEY, beforeHostname);
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.WORK_FOLDER_KEY, beforeWorkFolder);
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.PORT_KEY, String.valueOf(beforePort));

        var config = PropertiesPontonXPAdapterConfiguration.fromProperties(properties);
        assertEquals(beforeAdapterId, config.adapterId());
        assertEquals(beforeAdapterVersion, config.adapterVersion());
        assertEquals(beforeHostname, config.hostname());
        assertEquals(beforeWorkFolder, config.workFolder());
        assertEquals(beforePort, config.port());

        var afterAdapterId = "11";
        var afterAdapterVersion = "2.0.0";
        var afterHostname = "test";
        var afterWorkFolder = "test";
        var afterPort = 4321;
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.ADAPTER_ID_KEY, afterAdapterId);
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.ADAPTER_VERSION_KEY, afterAdapterVersion);
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.HOSTNAME_KEY, afterHostname);
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.WORK_FOLDER_KEY, afterWorkFolder);
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.PORT_KEY, String.valueOf(afterPort));

        assertEquals(afterAdapterId, config.adapterId());
        assertEquals(afterAdapterVersion, config.adapterVersion());
        assertEquals(afterHostname, config.hostname());
        assertEquals(afterWorkFolder, config.workFolder());
        assertEquals(afterPort, config.port());
    }

    @Test
    void port_whenPortPropertyIsRemoved_returnsDefaultPort() {
        Properties properties = new Properties();
        var expectedPort = 1234;
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.ADAPTER_ID_KEY, "9");
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.ADAPTER_VERSION_KEY, "1.0.0");
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.HOSTNAME_KEY, "localhost");
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.WORK_FOLDER_KEY, "work");
        properties.setProperty(PropertiesPontonXPAdapterConfiguration.PORT_KEY, String.valueOf(expectedPort));

        var config = PropertiesPontonXPAdapterConfiguration.fromProperties(properties);

        assertNotNull(config);
        assertEquals(expectedPort, config.port());

        properties.remove(PropertiesPontonXPAdapterConfiguration.PORT_KEY);
        assertEquals(PropertiesPontonXPAdapterConfiguration.DEFAULT_PORT, config.port());
    }

}