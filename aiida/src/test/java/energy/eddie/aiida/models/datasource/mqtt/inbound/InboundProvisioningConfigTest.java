// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.datasource.mqtt.inbound;

import energy.eddie.aiida.config.MqttConfiguration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InboundProvisioningConfigTest {
    private static final String INTERNAL_HOST = "mqtt://internal-broker";
    private static final String EXTERNAL_HOST = "mqtt://external-broker";
    private static final String USERNAME = "provisioning-user";
    private static final String PASSWORD = "provisioning-password";
    private static final String PASSWORD_HASH = "provisioning-password-hash";
    private static final String TOPIC = "aiida/inbound/test";

    @Test
    void establishClientModeConnection_storesConnectionAndAccessControlEntry() {
        var config = new InboundProvisioningConfig();

        var dto = config.establishClientModeConnection(INTERNAL_HOST, EXTERNAL_HOST, USERNAME, PASSWORD, TOPIC);

        assertThat(dto.host()).isEqualTo(INTERNAL_HOST);
        assertThat(dto.username()).isEqualTo(USERNAME);
        assertThat(dto.password()).isEqualTo(PASSWORD);
        assertThat(dto.topic()).isEqualTo(TOPIC);
        assertThat(config.connection()).isNotNull();
        assertThat(config.accessControlEntry()).isNotNull();
        assertThat(config.topic()).isEqualTo(TOPIC);
    }

    @Test
    void establishServerModeConnection_storesConnectionAndAccessControlEntry() {
        var mqttConfiguration = mock(MqttConfiguration.class);
        var config = new InboundProvisioningConfig();

        when(mqttConfiguration.internalHost()).thenReturn(INTERNAL_HOST);
        when(mqttConfiguration.externalHost()).thenReturn(EXTERNAL_HOST);

        var dto = config.establishServerModeConnection(
                mqttConfiguration,
                USERNAME,
                PASSWORD_HASH,
                PASSWORD,
                TOPIC
        );

        assertThat(dto.host()).isEqualTo(EXTERNAL_HOST);
        assertThat(dto.username()).isEqualTo(USERNAME);
        assertThat(dto.password()).isEqualTo(PASSWORD);
        assertThat(dto.topic()).isEqualTo(TOPIC);
        assertThat(config.connection()).isNotNull();
        assertThat(config.connection().password()).isEqualTo(PASSWORD_HASH);
        assertThat(config.accessControlEntry()).isNotNull();
        assertThat(config.topic()).isEqualTo(TOPIC);
    }

    @Test
    void clearMqttProvisioning_removesConnectionAndAccessControlEntry() {
        var config = new InboundProvisioningConfig();
        config.establishClientModeConnection(INTERNAL_HOST, EXTERNAL_HOST, USERNAME, PASSWORD, TOPIC);

        config.clearMqttProvisioning();

        assertThat(config.connection()).isNull();
        assertThat(config.accessControlEntry()).isNull();
        assertThat(config.topic()).isNull();
    }
}
