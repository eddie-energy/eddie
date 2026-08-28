// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.connectionlimit;

import energy.eddie.aiida.dtos.connectionlimit.ConnectionLimitDto;
import energy.eddie.aiida.errors.auth.InvalidUserException;
import energy.eddie.aiida.repositories.ConnectionLimitDefaultRepository;
import energy.eddie.aiida.repositories.ConnectionLimitRepository;
import energy.eddie.aiida.services.AuthService;
import energy.eddie.aiida.utils.ConnectionLimitCalculation;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ConnectionLimitService {
    private final ConnectionLimitRepository connectionLimitRepository;
    private final ConnectionLimitDefaultRepository connectionLimitDefaultRepository;
    private final AuthService authService;

    public ConnectionLimitService(
            ConnectionLimitRepository connectionLimitRepository,
            ConnectionLimitDefaultRepository connectionLimitDefaultRepository,
            AuthService authService
    ) {
        this.connectionLimitRepository = connectionLimitRepository;
        this.connectionLimitDefaultRepository = connectionLimitDefaultRepository;
        this.authService = authService;
    }

    public List<ConnectionLimitDto> getConnectionLimits(
            @Nullable UUID permissionId,
            @Nullable String meterId,
            Instant from,
            Instant to
    ) throws InvalidUserException {
        var currentUserId = authService.getCurrentUserId();

        if (from.isAfter(to)) {
            return List.of();
        }

        var limits = connectionLimitRepository.findByUserIdAndFiltersFromTo(currentUserId,
                                                                            permissionId,
                                                                            meterId,
                                                                            from,
                                                                            to);

        var defaults = connectionLimitDefaultRepository.findByUserIdAndPermissionId(currentUserId, permissionId);

        return new ConnectionLimitCalculation(limits, defaults, from, to).effectiveLimits();
    }
}
