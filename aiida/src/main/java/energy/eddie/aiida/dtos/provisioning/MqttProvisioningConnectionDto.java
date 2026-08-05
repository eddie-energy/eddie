// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.dtos.provisioning;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Connection details returned after MQTT provisioning is configured.
 *
 * @param host     MQTT broker host exposed to the provisioning client.
 * @param username MQTT username used for publishing.
 * @param password MQTT password used for publishing.
 * @param topic    Topic to which inbound records are published.
 */
public record MqttProvisioningConnectionDto(
        @JsonProperty("host")
        String host,

        @JsonProperty("username")
        String username,

        @JsonProperty("password")
        String password,

        @JsonProperty("topic")
        String topic
) {
    /**
     * Creates a connection DTO with empty values for provisioning types
     * that do not require connection details such as the REST options.
     *
     * @return A provisioning connection DTO whose fields are empty strings.
     */
    public static MqttProvisioningConnectionDto empty() {
        return new MqttProvisioningConnectionDto("", "", "", "");
    }
}
