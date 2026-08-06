// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.dtos.provisioning;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundProvisioningType;

import java.util.UUID;

/**
 * Request for changing the provisioning type of an inbound permission.
 *
 * @param permissionId ID of the permission to reconfigure.
 * @param type         Provisioning type to activate.
 * @param host         MQTT broker host used by client mode.
 * @param username     MQTT username used by client mode.
 * @param password     MQTT password used by client mode.
 * @param topic        MQTT topic used by client mode.
 */
public record ProvisioningTypePatchDto(
        @JsonProperty("permissionId")
        UUID permissionId,

        @JsonProperty("type")
        InboundProvisioningType type,

        @JsonProperty("host")
        String host,

        @JsonProperty("username")
        String username,

        @JsonProperty("password")
        String password,

        @JsonProperty("topic")
        String topic
) {
}
