// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.dtos.datasource.mqtt.inbound;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundProvisioningType;

import java.util.UUID;

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
