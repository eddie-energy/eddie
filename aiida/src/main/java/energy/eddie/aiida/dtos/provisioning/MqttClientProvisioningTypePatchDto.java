// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.dtos.provisioning;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundProvisioningType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request for activating MQTT client provisioning.
 *
 * @param type     Provisioning type to activate.
 * @param host     MQTT broker host.
 * @param username MQTT username.
 * @param password MQTT password.
 * @param topic    MQTT topic.
 */
public record MqttClientProvisioningTypePatchDto(
        @JsonProperty("type")
        @NotNull(message = "must not be null")
        InboundProvisioningType type,

        @JsonProperty("host")
        @NotBlank(message = "must not be blank")
        String host,

        @JsonProperty("username")
        @NotBlank(message = "must not be blank")
        String username,

        @JsonProperty("password")
        @NotBlank(message = "must not be blank")
        String password,

        @JsonProperty("topic")
        @NotBlank(message = "must not be blank")
        String topic
) implements ProvisioningTypePatchDto {
}
