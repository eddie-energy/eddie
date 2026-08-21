// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.dtos.provisioning;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundProvisioningType;
import jakarta.validation.constraints.NotNull;

/**
 * Request for activating a provisioning mode that needs no client-supplied connection details.
 *
 * @param type Provisioning type to activate.
 */
public record ProvisioningTypeSelectionPatchDto(
        @JsonProperty("type")
        @NotNull(message = "must not be null")
        InboundProvisioningType type
) implements ProvisioningTypePatchDto {
}
