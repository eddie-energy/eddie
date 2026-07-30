// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.datasource.it;

import energy.eddie.aiida.config.datasource.it.SinapsiAlfaConfiguration;
import energy.eddie.aiida.dtos.datasource.mqtt.it.SinapsiAlfaDataSourceDto;
import energy.eddie.aiida.errors.datasource.mqtt.it.SinapsiAlfaEmptyConfigException;
import energy.eddie.aiida.models.datasource.mqtt.it.SinapsiAlfaDataSource;
import energy.eddie.aiida.services.secrets.KeyStoreSecretsService;
import energy.eddie.aiida.services.secrets.SecretType;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SinapsiAlfaDataSourceTest {
    @Test
    void configure_throwsException_whenUsernameIsMissing() {
        // Given
        var dataSource = new SinapsiAlfaDataSource(mock(SinapsiAlfaDataSourceDto.class), UUID.randomUUID());
        var config = mock(SinapsiAlfaConfiguration.class);
        when(config.mqttUsername()).thenReturn("");
        when(config.mqttPassword()).thenReturn("password");

        // When / Then
        assertThrows(
                SinapsiAlfaEmptyConfigException.class,
                () -> dataSource.configure(config, "key")
        );
    }

    @Test
    void configure_throwsException_whenPasswordIsMissing() {
        // Given
        var dataSource = new SinapsiAlfaDataSource(mock(SinapsiAlfaDataSourceDto.class), UUID.randomUUID());
        var config = mock(SinapsiAlfaConfiguration.class);
        when(config.mqttUsername()).thenReturn("username");
        when(config.mqttPassword()).thenReturn("");

        // When / Then
        assertThrows(
                SinapsiAlfaEmptyConfigException.class,
                () -> dataSource.configure(config, "key")
        );
    }

    @Test
    void configure_setsMqttConnection_whenValid() {
        // Given
        var dataSource = new SinapsiAlfaDataSource(mock(SinapsiAlfaDataSourceDto.class), UUID.randomUUID());
        var config = mock(SinapsiAlfaConfiguration.class);
        when(config.mqttHost()).thenReturn("mqtt://broker");
        when(config.mqttUsername()).thenReturn("username");
        when(config.mqttPassword()).thenReturn("password");

        // When / Then
        assertDoesNotThrow(() -> dataSource.configure(config, "key"));
    }

    @Test
    void createMqttUser_setsPasswordToKeystoreAlias() throws Exception {
        // Given
        var id = UUID.randomUUID();
        var dto = mock(SinapsiAlfaDataSourceDto.class);
        when(dto.id()).thenReturn(id);
        var dataSource = new SinapsiAlfaDataSource(dto, UUID.randomUUID());
        var config = mock(SinapsiAlfaConfiguration.class);
        when(config.mqttHost()).thenReturn("mqtt://broker");
        when(config.mqttUsername()).thenReturn("username");
        when(config.mqttPassword()).thenReturn("password");
        dataSource.configure(config, "key");

        // When
        invokeProtected(dataSource, "createMqttUser");

        // Then
        assertEquals(KeyStoreSecretsService.alias(id, SecretType.PASSWORD), dataSource.password());
    }

    @Test
    void createAccessControlEntry_buildsExpectedTopic() throws Exception {
        // Given
        var dto = mock(SinapsiAlfaDataSourceDto.class);
        when(dto.id()).thenReturn(UUID.randomUUID());
        var dataSource = new SinapsiAlfaDataSource(dto, UUID.randomUUID());
        var config = mock(SinapsiAlfaConfiguration.class);
        when(config.mqttHost()).thenReturn("mqtt://broker");
        when(config.mqttUsername()).thenReturn("username");
        when(config.mqttPassword()).thenReturn("password");
        dataSource.configure(config, "activation-key");

        // When
        invokeProtected(dataSource, "createAccessControlEntry");

        // Then
        assertEquals(SinapsiAlfaConfiguration.TOPIC_PREFIX
                     + "username"
                     + SinapsiAlfaConfiguration.TOPIC_INFIX
                     + "activation-key"
                     + SinapsiAlfaConfiguration.TOPIC_SUFFIX,
                     dataSource.topic());
    }

    @Test
    void updatePassword_isNoOp() {
        // Given
        var dataSource = new SinapsiAlfaDataSource(mock(SinapsiAlfaDataSourceDto.class), UUID.randomUUID());
        var encoder = mock(BCryptPasswordEncoder.class);

        // When
        dataSource.updatePassword(encoder, "new-password");

        // Then
        verifyNoInteractions(encoder);
    }

    private static void invokeProtected(SinapsiAlfaDataSource dataSource, String methodName) throws Exception {
        Method method = SinapsiAlfaDataSource.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(dataSource);
    }
}
