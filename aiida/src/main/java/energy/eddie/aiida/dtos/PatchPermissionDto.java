// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.dtos;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import energy.eddie.aiida.models.permission.InboundMessageFormat;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

import static energy.eddie.aiida.dtos.PatchPermissionDto.*;

/**
 * This data transfer object is expected by the updatePermission API endpoint.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "operation"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Accept.class, name = "ACCEPT"),
        @JsonSubTypes.Type(value = Reject.class, name = "REJECT"),
        @JsonSubTypes.Type(value = Revoke.class, name = "REVOKE"),
        @JsonSubTypes.Type(value = UpdateInboundMessageFormat.class, name = "UPDATE_INBOUND_MESSAGE_FORMAT")
})
public interface PatchPermissionDto {
    record Accept(
            @Nullable UUID dataSourceId,
            @Nullable InboundMessageFormat inboundMessageFormat
    ) implements PatchPermissionDto {}

    record Reject() implements PatchPermissionDto {}

    record Revoke() implements PatchPermissionDto {}

    record UpdateInboundMessageFormat(
            @NotNull InboundMessageFormat inboundMessageFormat
    ) implements PatchPermissionDto {}
}
