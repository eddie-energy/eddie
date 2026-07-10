// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.connectionlimit;

import energy.eddie.aiida.dtos.connectionlimit.ConnectionLimitDto;
import energy.eddie.aiida.errors.auth.InvalidUserException;
import energy.eddie.aiida.errors.connectionlimit.PermissionDoesNotSupportConnectionLimitsException;
import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.aiida.models.connectionlimit.ConnectionLimit;
import energy.eddie.aiida.models.permission.dataneed.InboundAiidaLocalDataNeed;
import energy.eddie.aiida.repositories.ConnectionLimitRepository;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.services.AuthService;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ConnectionLimitService {
    private final ConnectionLimitRepository connectionLimitRepository;
    private final PermissionRepository permissionRepository;
    private final AuthService authService;
    private final Clock clock;

    public ConnectionLimitService(
            ConnectionLimitRepository connectionLimitRepository,
            PermissionRepository permissionRepository,
            AuthService authService,
            Clock clock
    ) {
        this.connectionLimitRepository = connectionLimitRepository;
        this.permissionRepository = permissionRepository;
        this.authService = authService;
        this.clock = clock;
    }

    public List<ConnectionLimitDto> getConnectionLimits(
            UUID permissionId,
            Instant from,
            Instant to,
            Integer offset
    ) throws InvalidUserException, PermissionNotFoundException, PermissionDoesNotSupportConnectionLimitsException {
        var currentUserId = authService.getCurrentUserId();
        var permission = permissionRepository.findByPermissionIdAndUserId(permissionId, currentUserId)
                                             .orElseThrow(() -> new PermissionNotFoundException(permissionId));

        var dataNeed = permission.dataNeed();
        var supportsConnectionLimits = dataNeed instanceof InboundAiidaLocalDataNeed
                                       && dataNeed.schemas() != null
                                       && dataNeed.schemas().contains(AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12);
        if (!supportsConnectionLimits) {
            throw new PermissionDoesNotSupportConnectionLimitsException(permissionId);
        }

        var now = clock.instant();
        var fromResolved = from != null ? from : now;
        var toResolved = offset == null && to == null ? now : to;

        if (toResolved != null && fromResolved.isAfter(toResolved)) {
            return List.of();
        }

        var pageable = offset == null ? Pageable.unpaged() : PageRequest.ofSize(offset + 1);
        var limits = connectionLimitRepository.findByPermissionIdFromTo(permissionId,
                                                                        fromResolved,
                                                                        toResolved,
                                                                        pageable);
        return toDto(limits);
    }

    private List<ConnectionLimitDto> toDto(List<ConnectionLimit> limits) {
        return limits.stream()
                     .map(limit -> new ConnectionLimitDto(
                             limit.permissionId(),
                             limit.meterId(),
                             limit.intervalStart(),
                             limit.intervalEnd(),
                             limit.minLimitKw(),
                             limit.maxLimitKw()
                     ))
                     .toList();
    }
}
