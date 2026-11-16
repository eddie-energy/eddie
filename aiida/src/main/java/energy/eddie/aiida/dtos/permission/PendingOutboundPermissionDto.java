// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.dtos.permission;

import energy.eddie.aiida.models.permission.OutboundPermission;
import energy.eddie.aiida.models.permission.dataneed.OutboundAiidaLocalDataNeed;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.UUID;

/**
 * Details of an outbound permission request, see {@link PendingPermissionDto}.
 */
public record PendingOutboundPermissionDto(
        UUID permissionId,
        UUID eddieId,
        String serviceName,
        String displayName,
        Instant startTime,
        Instant expirationTime,
        @Nullable String meterId,
        OutboundAiidaLocalDataNeed dataNeed
) implements PendingPermissionDto {
    /**
     * Creates the DTO from an outbound permission that was built from the fetched details.
     */
    public static PendingOutboundPermissionDto from(OutboundPermission permission) {
        return new PendingOutboundPermissionDto(permission.id(),
                                                permission.eddieId(),
                                                permission.serviceName(),
                                                permission.displayName(),
                                                permission.startTime(),
                                                permission.expirationTime(),
                                                permission.meterId(),
                                                permission.dataNeed());
    }
}
