// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.dtos.permission;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import energy.eddie.aiida.models.permission.InboundPermission;
import energy.eddie.aiida.models.permission.OutboundPermission;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.dataneed.AiidaLocalDataNeed;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.UUID;

/**
 * Details of a permission request that was fetched from the EDDIE framework and validated, but is not yet accepted
 * and therefore not persisted. The {@code type} property tells inbound and outbound permission requests apart.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = PermissionDto.TYPE_PROPERTY)
@JsonSubTypes({
        @JsonSubTypes.Type(value = PendingInboundPermissionDto.class, name = InboundPermission.DISCRIMINATOR_VALUE),
        @JsonSubTypes.Type(value = PendingOutboundPermissionDto.class, name = OutboundPermission.DISCRIMINATOR_VALUE)
})
@Schema(
        oneOf = {PendingInboundPermissionDto.class, PendingOutboundPermissionDto.class},
        discriminatorProperty = PermissionDto.TYPE_PROPERTY,
        discriminatorMapping = {
                @DiscriminatorMapping(value = InboundPermission.DISCRIMINATOR_VALUE, schema = PendingInboundPermissionDto.class),
                @DiscriminatorMapping(value = OutboundPermission.DISCRIMINATOR_VALUE, schema = PendingOutboundPermissionDto.class)
        }
)
public sealed interface PendingPermissionDto permits PendingInboundPermissionDto, PendingOutboundPermissionDto {
    /**
     * Creates the DTO matching the direction of a permission that was built from the fetched details.
     */
    static PendingPermissionDto from(Permission permission) {
        return switch (permission) {
            case InboundPermission inboundPermission -> PendingInboundPermissionDto.from(inboundPermission);
            case OutboundPermission outboundPermission -> PendingOutboundPermissionDto.from(outboundPermission);
            default -> throw new IllegalArgumentException("Unsupported permission type " + permission.getClass().getName());
        };
    }

    UUID permissionId();

    UUID eddieId();

    String serviceName();

    String displayName();

    Instant startTime();

    Instant expirationTime();

    @Nullable
    String meterId();

    AiidaLocalDataNeed dataNeed();
}
