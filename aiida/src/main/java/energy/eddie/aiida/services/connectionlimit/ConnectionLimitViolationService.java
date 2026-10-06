// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.connectionlimit;

import energy.eddie.aiida.errors.auth.InvalidUserException;
import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.aiida.models.connectionlimit.ConnectionLimitViolation;
import energy.eddie.aiida.repositories.ConnectionLimitViolationRepository;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.services.AuthService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ConnectionLimitViolationService {
    private final ConnectionLimitViolationRepository connectionLimitViolationRepository;
    private final PermissionRepository permissionRepository;
    private final AuthService authService;

    public ConnectionLimitViolationService(
            ConnectionLimitViolationRepository connectionLimitViolationRepository,
            PermissionRepository permissionRepository,
            AuthService authService
    ) {
        this.connectionLimitViolationRepository = connectionLimitViolationRepository;
        this.permissionRepository = permissionRepository;
        this.authService = authService;
    }

    /**
     * Returns the violations of the permission that overlap with the time frame, ordered by their start.
     */
    public List<ConnectionLimitViolation> getViolations(
            UUID permissionId,
            Instant from,
            Instant to
    ) throws InvalidUserException, PermissionNotFoundException {
        if (permissionRepository.findByPermissionIdAndUserId(permissionId, authService.getCurrentUserId()).isEmpty()) {
            throw new PermissionNotFoundException(permissionId);
        }

        return connectionLimitViolationRepository.findOverlapping(permissionId, from, to);
    }
}
