// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.connectionlimit;

import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.aiida.models.connectionlimit.ConnectionLimitViolation;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.repositories.ConnectionLimitViolationRepository;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.services.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConnectionLimitViolationServiceTest {
    private static final UUID USER_ID = UUID.fromString("092bf5cb-8571-4313-9429-8caf5e679f6e");
    private static final UUID PERMISSION_ID = UUID.fromString("9921f327-f341-4bea-bf08-3cf2acc65bf3");
    private static final Instant FROM = Instant.parse("2026-07-10T08:00:00Z");
    private static final Instant TO = Instant.parse("2026-07-10T10:00:00Z");

    @Mock
    private ConnectionLimitViolationRepository connectionLimitViolationRepository;
    @Mock
    private PermissionRepository permissionRepository;
    @Mock
    private AuthService authService;

    private ConnectionLimitViolationService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new ConnectionLimitViolationService(connectionLimitViolationRepository,
                                                      permissionRepository,
                                                      authService);
        when(authService.getCurrentUserId()).thenReturn(USER_ID);
    }

    @Test
    void givenOwnedPermission_returnsViolations() throws Exception {
        var violation = new ConnectionLimitViolation(PERMISSION_ID,
                                                     UUID.randomUUID(),
                                                     FROM,
                                                     "document",
                                                     BigDecimal.valueOf(3),
                                                     BigDecimal.valueOf(8),
                                                     BigDecimal.valueOf(9));
        when(permissionRepository.findByPermissionIdAndUserId(PERMISSION_ID, USER_ID))
                .thenReturn(Optional.of(mock(Permission.class)));
        when(connectionLimitViolationRepository.findOverlapping(PERMISSION_ID, FROM, TO))
                .thenReturn(List.of(violation));

        var result = service.getViolations(PERMISSION_ID, FROM, TO);

        assertEquals(List.of(violation), result);
    }

    @Test
    void givenForeignPermission_throwsNotFound() {
        when(permissionRepository.findByPermissionIdAndUserId(PERMISSION_ID, USER_ID)).thenReturn(Optional.empty());

        assertThrows(PermissionNotFoundException.class, () -> service.getViolations(PERMISSION_ID, FROM, TO));
    }
}
