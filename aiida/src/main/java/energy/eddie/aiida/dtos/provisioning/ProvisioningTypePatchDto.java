// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.dtos.provisioning;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundProvisioningType;

/**
 * Request for changing the provisioning type of an inbound permission.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true,
        defaultImpl = ProvisioningTypeSelectionPatchDto.class
)
@JsonSubTypes({
        @JsonSubTypes.Type(
                value = MqttClientProvisioningTypePatchDto.class,
                name = InboundProvisioningType.Identifiers.MQTT_CLIENT
        )
})
public sealed interface ProvisioningTypePatchDto
        permits MqttClientProvisioningTypePatchDto, ProvisioningTypeSelectionPatchDto {

    InboundProvisioningType type();
}
