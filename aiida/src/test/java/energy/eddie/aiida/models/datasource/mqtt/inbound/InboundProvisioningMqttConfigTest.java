// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.datasource.mqtt.inbound;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InboundProvisioningMqttConfigTest {
    private static final String INTERNAL_HOST = "mqtt://internal-broker";
    private static final String EXTERNAL_HOST = "mqtt://external-broker";
    private static final String USERNAME = "provisioning-user";
    private static final String PASSWORD = "provisioning-password";
    private static final String TOPIC = "aiida/inbound/test";

    @Test
    void create_storesConnectionAndAccessControlEntry() {
        var config = InboundProvisioningMqttConfig.create(
                INTERNAL_HOST,
                EXTERNAL_HOST,
                USERNAME,
                PASSWORD,
                TOPIC
        );

        assertThat(config.connection().internalHost()).isEqualTo(INTERNAL_HOST);
        assertThat(config.connection().username()).isEqualTo(USERNAME);
        assertThat(config.connection().password()).isEqualTo(PASSWORD);
        assertThat(config.accessControlEntry().topic()).isEqualTo(TOPIC);
        assertThat(config.topic()).isEqualTo(TOPIC);
    }
}
