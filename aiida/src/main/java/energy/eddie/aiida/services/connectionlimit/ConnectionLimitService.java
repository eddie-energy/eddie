// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.connectionlimit;

import energy.eddie.aiida.dtos.connectionlimit.ConnectionLimitDto;
import energy.eddie.aiida.errors.auth.InvalidUserException;
import energy.eddie.aiida.models.connectionlimit.ConnectionLimit;
import energy.eddie.aiida.repositories.ConnectionLimitRepository;
import energy.eddie.aiida.services.AuthService;
import jakarta.annotation.Nullable;
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

        var pageable = offset == null ? Pageable.unpaged() : PageRequest.ofSize(offset + 1);
        var limits = connectionLimitRepository.findByUserIdAndFiltersFromTo(currentUserId,
                                                                            permissionId,
                                                                            meterId,
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
