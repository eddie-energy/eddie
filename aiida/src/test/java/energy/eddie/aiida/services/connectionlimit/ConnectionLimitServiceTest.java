// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.connectionlimit;

import energy.eddie.aiida.repositories.ConnectionLimitRepository;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.services.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConnectionLimitServiceTest {
    private static final UUID USER_ID = UUID.fromString("092bf5cb-8571-4313-9429-8caf5e679f6e");

    @Mock
    private ConnectionLimitRepository connectionLimitRepository;
    @Mock
    private PermissionRepository permissionRepository;
    @Mock
    private AuthService authService;

    private ConnectionLimitService service;

    @BeforeEach
    void setUp() {
        service = new ConnectionLimitService(connectionLimitRepository, permissionRepository, authService);
    }

    @Test
    void givenFromAfterTo_returnsEmptyWithoutRepositoryCall() throws Exception {
        when(authService.getCurrentUserId()).thenReturn(USER_ID);

        var result = service.getConnectionLimits(null,
                                                 null,
                                                 Instant.parse("2026-07-10T09:00:00Z"),
                                                 Instant.parse("2026-07-10T08:00:00Z"));

        assertTrue(result.isEmpty());
        verify(connectionLimitRepository, never()).findByUserIdAndFiltersFromTo(any(), any(), any(), any(), any());
    }
}
