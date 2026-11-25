// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.dtos.permission;

import energy.eddie.aiida.models.permission.InboundMessageFormat;
import energy.eddie.aiida.models.permission.InboundPermission;
import energy.eddie.aiida.models.permission.dataneed.InboundAiidaLocalDataNeed;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Details of an inbound permission request, see {@link PendingPermissionDto}.
 */
public record PendingInboundPermissionDto(
        UUID permissionId,
        UUID eddieId,
        String serviceName,
        String displayName,
        Instant startTime,
        Instant expirationTime,
        @Nullable String meterId,
        InboundAiidaLocalDataNeed dataNeed,
        @Nullable BigDecimal minLimitKw,
        @Nullable BigDecimal maxLimitKw,
        InboundMessageFormat inboundMessageFormat,
        boolean supportsConnectionLimits
) implements PendingPermissionDto {
    /**
     * Creates the DTO from an inbound permission that was built from the fetched details.
     */
    public static PendingInboundPermissionDto from(InboundPermission permission) {
        return new PendingInboundPermissionDto(permission.id(),
                                               permission.eddieId(),
                                               permission.serviceName(),
                                               permission.displayName(),
                                               permission.startTime(),
                                               permission.expirationTime(),
                                               permission.meterId(),
                                               permission.dataNeed(),
                                               permission.minLimitKw(),
                                               permission.maxLimitKw(),
                                               permission.inboundMessageFormat(),
                                               permission.supportsConnectionLimits());
    }
}
