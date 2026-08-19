// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.connectionlimit;

import energy.eddie.aiida.dtos.connectionlimit.ConnectionLimitDto;
import energy.eddie.aiida.errors.auth.InvalidUserException;
import energy.eddie.aiida.repositories.ConnectionLimitRepository;
import energy.eddie.aiida.services.AuthService;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ConnectionLimitService {
    private final ConnectionLimitRepository connectionLimitRepository;
    private final AuthService authService;
    private final Clock clock;

    public ConnectionLimitService(
            ConnectionLimitRepository connectionLimitRepository,
            AuthService authService,
            Clock clock
    ) {
        this.connectionLimitRepository = connectionLimitRepository;
        this.authService = authService;
        this.clock = clock;
    }

    public List<ConnectionLimitDto> getConnectionLimits(
            @Nullable UUID permissionId,
            @Nullable String meterId,
            Instant from,
            Instant to,
            Integer offset
    ) throws InvalidUserException {
        var currentUserId = authService.getCurrentUserId();

        var now = clock.instant();
        var fromResolved = from != null ? from : now;
        var toResolved = offset == null && to == null ? now : to;

        if (toResolved != null && fromResolved.isAfter(toResolved)) {
            return List.of();
        }

        var limit = offset == null ? null : offset + 1;
        var effectiveLimits = connectionLimitRepository.findEffectiveByUserIdAndFiltersFromTo(currentUserId,
                                                                                              permissionId,
                                                                                              meterId,
                                                                                              fromResolved,
                                                                                              toResolved,
                                                                                              limit);

        return effectiveLimits.stream().map(this::toDto).toList();
    }

    private ConnectionLimitDto toDto(ConnectionLimitRepository.EffectiveConnectionLimitProjection limit) {
        return new ConnectionLimitDto(limit.getPermissionId(),
                                      limit.getMeterId().isBlank() ? null : limit.getMeterId(),
                                      limit.getIntervalStart(),
                                      limit.getIntervalEnd(),
                                      limit.getMinLimitKw(),
                                      limit.getMaxLimitKw());
    }
}
