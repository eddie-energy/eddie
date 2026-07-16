// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.permission;

import energy.eddie.aiida.services.secrets.KeyStoreSecretsService;
import energy.eddie.aiida.services.secrets.SecretType;
import energy.eddie.api.agnostic.aiida.mqtt.MqttDto;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MqttStreamingConfigTest {
    private static final UUID PERMISSION_ID = UUID.randomUUID();
    private static final MqttDto MQTT_DTO = new MqttDto(
            "mqtt://broker",
            PERMISSION_ID.toString(),
            "plaintext-password",
            "data-topic",
            "status-topic",
            "command-topic",
            "acknowledgement-topic"
    );

    @Test
    void constructor_setsPasswordToKeystoreAliasKeyedByPermissionId() {
        // When
        var config = new MqttStreamingConfig(MQTT_DTO);

        // Then
        assertEquals(KeyStoreSecretsService.alias(PERMISSION_ID, SecretType.PASSWORD), config.password());
        assertEquals(PERMISSION_ID, config.permissionId());
        assertEquals(PERMISSION_ID, config.username());
    }

    @Test
    void updatePassword_replacesStoredValue() {
        // Given
        var config = new MqttStreamingConfig(MQTT_DTO);

        // When
        config.updatePassword("new-alias");

        // Then
        assertEquals("new-alias", config.password());
    }

    @Test
    void gettersReturnConstructedValues() {
        // When
        var config = new MqttStreamingConfig(MQTT_DTO);

        // Then
        assertEquals("mqtt://broker", config.serverUri());
        assertEquals("data-topic", config.dataTopic());
        assertEquals("status-topic", config.statusTopic());
        assertEquals("command-topic", config.commandTopic());
        assertEquals("acknowledgement-topic", config.acknowledgementTopic());
    }
}
