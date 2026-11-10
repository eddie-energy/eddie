// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services;

import energy.eddie.aiida.models.permission.ConnectionStatus;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import energy.eddie.aiida.streamers.StreamerManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConnectionStatusServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-03T10:00:00Z");
    private final UUID permissionId = UUID.randomUUID();
    @Mock
    private StreamerManager streamerManager;
    private ConnectionStatusService service;
    private Permission permission;

    @BeforeEach
    void setUp() {
        service = new ConnectionStatusService(Clock.fixed(NOW, ZoneOffset.UTC), streamerManager);
        permission = new Permission(UUID.randomUUID(), permissionId, "https://example.org", "token", UUID.randomUUID());
    }

    @ParameterizedTest
    @EnumSource(value = PermissionStatus.class, names = "ACCEPTED", mode = EnumSource.Mode.EXCLUDE)
    void givenNotAcceptedPermission_returnsNull(PermissionStatus status) {
        // Given
        permission.setStatus(status);

        // When
        var connectionStatus = service.connectionStatusOf(permission);

        // Then
        assertNull(connectionStatus);
        verifyNoInteractions(streamerManager);
    }

    @Test
    void givenAcceptedPermissionWithFutureStart_returnsWaitingForConnection() {
        // Given
        permission.setStatus(PermissionStatus.ACCEPTED);
        permission.setStartTime(NOW.plusSeconds(60));

        // When
        var connectionStatus = service.connectionStatusOf(permission);

        // Then
        assertEquals(ConnectionStatus.WAITING_FOR_CONNECTION, connectionStatus);
        verifyNoInteractions(streamerManager);
    }

    @Test
    void givenAcceptedPermissionWithConnectedStreamer_returnsConnected() {
        // Given
        permission.setStatus(PermissionStatus.ACCEPTED);
        permission.setStartTime(NOW.minusSeconds(60));
        when(streamerManager.isConnected(permissionId)).thenReturn(true);

        // When
        var connectionStatus = service.connectionStatusOf(permission);

        // Then
        assertEquals(ConnectionStatus.CONNECTED, connectionStatus);
    }

    @Test
    void givenAcceptedPermissionWithoutConnectedStreamer_returnsNotConnected() {
        // Given
        permission.setStatus(PermissionStatus.ACCEPTED);
        permission.setStartTime(NOW);
        when(streamerManager.isConnected(permissionId)).thenReturn(false);

        // When
        var connectionStatus = service.connectionStatusOf(permission);

        // Then
        assertEquals(ConnectionStatus.NOT_CONNECTED, connectionStatus);
    }

    @Test
    void toDto_includesDerivedConnectionStatus() {
        // Given
        permission.setStatus(PermissionStatus.ACCEPTED);
        permission.setStartTime(NOW.minusSeconds(60));
        when(streamerManager.isConnected(permissionId)).thenReturn(true);

        // When
        var result = service.toDto(permission);

        // Then
        assertEquals(permissionId, result.permissionId());
        assertEquals(PermissionStatus.ACCEPTED, result.status());
        assertEquals(ConnectionStatus.CONNECTED, result.connectionStatus());
    }
}
