// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.connectionlimit;

import energy.eddie.aiida.errors.auth.InvalidUserException;
import energy.eddie.aiida.errors.connectionlimit.PermissionDoesNotSupportConnectionLimitsException;
import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.aiida.models.connectionlimit.ConnectionLimit;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.dataneed.AiidaLocalDataNeed;
import energy.eddie.aiida.models.permission.dataneed.InboundAiidaLocalDataNeed;
import energy.eddie.aiida.repositories.ConnectionLimitRepository;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.services.AuthService;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConnectionLimitServiceTest {
    private static final UUID PERMISSION_ID = UUID.fromString("9921f327-f341-4bea-bf08-3cf2acc65bf3");
    private static final UUID USER_ID = UUID.fromString("092bf5cb-8571-4313-9429-8caf5e679f6e");

    @Mock
    private ConnectionLimitRepository connectionLimitRepository;
    @Mock
    private PermissionRepository permissionRepository;
    @Mock
    private AuthService authService;
    @Mock
    private Permission permission;
    @Mock
    private InboundAiidaLocalDataNeed inboundDataNeed;

    private ConnectionLimitService service;

    @BeforeEach
    void setUp() {
        var fixedClock = Clock.fixed(Instant.parse("2026-07-10T08:45:50Z"), ZoneOffset.UTC);
        service = new ConnectionLimitService(connectionLimitRepository, permissionRepository, authService, fixedClock);
    }

    @Test
    void givenNoQueryParams_queriesCurrentInterval() throws Exception {
        var now = Instant.parse("2026-07-10T08:45:50Z");
        var limit = new ConnectionLimit(
                PERMISSION_ID,
                "003114735",
                Instant.parse("2026-07-10T08:45:00Z"),
                Instant.parse("2026-07-10T09:00:00Z"),
                new BigDecimal("3.0"),
                new BigDecimal("8.0")
        );
        mockSupportedPermission();
        when(connectionLimitRepository.findByPermissionIdFromTo(eq(PERMISSION_ID), eq(now), eq(now), any()))
                .thenReturn(List.of(limit));

        var result = service.getConnectionLimits(PERMISSION_ID, null, null, null);

        assertEquals(1, result.size());
        assertEquals(PERMISSION_ID, result.getFirst().permissionId());
        assertEquals(Instant.parse("2026-07-10T08:45:00Z"), result.getFirst().intervalStart());
        verify(connectionLimitRepository).findByPermissionIdFromTo(eq(PERMISSION_ID), eq(now), eq(now), any());
    }

    @Test
    void givenFromAndOffset_queriesNextNLimits() throws Exception {
        var from = Instant.parse("2026-07-10T10:00:00Z");
        mockSupportedPermission();
        when(connectionLimitRepository.findByPermissionIdFromTo(any(), any(), any(), any()))
                .thenReturn(List.of());

        service.getConnectionLimits(PERMISSION_ID, from, null, 2);

        var fromCaptor = ArgumentCaptor.forClass(Instant.class);
        var toCaptor = ArgumentCaptor.forClass(Instant.class);
        var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(connectionLimitRepository).findByPermissionIdFromTo(eq(PERMISSION_ID),
                                                                   fromCaptor.capture(),
                                                                   toCaptor.capture(),
                                                                   pageableCaptor.capture());
        assertEquals(from, fromCaptor.getValue());
        assertNull(toCaptor.getValue());
        assertEquals(3, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void givenFromInFutureWithoutTo_returnsEmpty() throws Exception {
        var from = Instant.parse("2026-07-10T09:00:00Z");
        mockSupportedPermission();

        var result = service.getConnectionLimits(PERMISSION_ID, from, null, null);

        assertTrue(result.isEmpty());
        verify(connectionLimitRepository, never()).findByPermissionIdFromTo(any(), any(), any(), any());
    }

    @Test
    void givenPermissionWithoutMinMaxSchema_throwsUnprocessableException() throws Exception {
        when(authService.getCurrentUserId()).thenReturn(USER_ID);
        when(permissionRepository.findByPermissionIdAndUserId(PERMISSION_ID, USER_ID)).thenReturn(Optional.of(permission));
        var dataNeed = mock(AiidaLocalDataNeed.class);
        when(permission.dataNeed()).thenReturn(dataNeed);

        assertThrows(PermissionDoesNotSupportConnectionLimitsException.class,
                     () -> service.getConnectionLimits(PERMISSION_ID, null, null, null));
    }

    @Test
    void givenOtherUsersPermission_throwsNotFound() throws Exception {
        var otherUserId = UUID.fromString("45ef8cce-f66a-46e4-a1fb-35ffef63b57d");
        when(authService.getCurrentUserId()).thenReturn(otherUserId);
        when(permissionRepository.findByPermissionIdAndUserId(PERMISSION_ID, otherUserId)).thenReturn(Optional.empty());

        assertThrows(PermissionNotFoundException.class,
                     () -> service.getConnectionLimits(PERMISSION_ID, null, null, null));
    }

    private void mockSupportedPermission() throws InvalidUserException {
        when(authService.getCurrentUserId()).thenReturn(USER_ID);
        when(permissionRepository.findByPermissionIdAndUserId(PERMISSION_ID, USER_ID)).thenReturn(Optional.of(permission));
        when(permission.dataNeed()).thenReturn(inboundDataNeed);
        when(inboundDataNeed.schemas()).thenReturn(Set.of(AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12));
    }
}
