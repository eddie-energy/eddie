// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.dtos.permission;

import energy.eddie.aiida.models.permission.InboundMessageFormat;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.dataneed.AiidaLocalDataNeed;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * Details of a permission request that was fetched from the EDDIE framework and validated, but is not yet accepted
 * and therefore not persisted.
 */
public record PendingPermissionDto(
        UUID permissionId,
        UUID eddieId,
        @Nullable String serviceName,
        @Nullable String displayName,
        Instant startTime,
        Instant expirationTime,
        @Nullable String meterId,
        @Nullable BigDecimal minLimitKw,
        @Nullable BigDecimal maxLimitKw,
        AiidaLocalDataNeed dataNeed,
        @Nullable InboundMessageFormat inboundMessageFormat,
        boolean supportsConnectionLimits
) {
    /**
     * Creates the DTO from a permission that was built from the fetched details.
     */
    public static PendingPermissionDto from(Permission permission) {
        return new PendingPermissionDto(permission.id(),
                                        permission.eddieId(),
                                        permission.serviceName(),
                                        permission.displayName(),
                                        requireNonNull(permission.startTime()),
                                        requireNonNull(permission.expirationTime()),
                                        permission.meterId(),
                                        permission.minLimitKw(),
                                        permission.maxLimitKw(),
                                        requireNonNull(permission.dataNeed()),
                                        permission.inboundMessageFormat(),
                                        permission.supportsConnectionLimits());
    }
}
