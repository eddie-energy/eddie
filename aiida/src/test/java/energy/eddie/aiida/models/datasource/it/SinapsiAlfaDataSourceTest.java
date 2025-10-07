package energy.eddie.aiida.models.datasource.it;

import energy.eddie.aiida.config.datasource.it.SinapsiAlfaConfiguration;
import energy.eddie.aiida.dtos.datasource.mqtt.it.SinapsiAlfaDataSourceDto;
import energy.eddie.aiida.errors.SinapsiAlflaEmptyConfigException;
import energy.eddie.aiida.models.datasource.mqtt.it.SinapsiAlfaDataSource;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SinapsiAlfaDataSourceTest {
    @Test
    void generateMqttSettings_throwsException_whenUsernameIsMissing() {
        // Given
        var dataSource = new SinapsiAlfaDataSource(mock(SinapsiAlfaDataSourceDto.class), UUID.randomUUID());
        var config = mock(SinapsiAlfaConfiguration.class);
        when(config.mqttUsername()).thenReturn("");
        when(config.mqttPassword()).thenReturn("password");

        // When / Then
        assertThrows(
                SinapsiAlflaEmptyConfigException.class,
                () -> dataSource.generateMqttSettings(config, "key")
        );
    }

    @Test
    void generateMqttSettings_throwsException_whenPasswordIsMissing() {
        // Given
        var dataSource = new SinapsiAlfaDataSource(mock(SinapsiAlfaDataSourceDto.class), UUID.randomUUID());
        var config = mock(SinapsiAlfaConfiguration.class);
        when(config.mqttUsername()).thenReturn("username");
        when(config.mqttPassword()).thenReturn("");

        // When / Then
        assertThrows(
                SinapsiAlflaEmptyConfigException.class,
                () -> dataSource.generateMqttSettings(config, "key")
        );
    }
}
