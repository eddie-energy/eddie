// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.utils;

import energy.eddie.aiida.config.AiidaConfiguration;
import energy.eddie.aiida.models.permission.AcceptedPermission;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import energy.eddie.aiida.models.permission.dataneed.AiidaLocalDataNeed;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.streamers.StreamerManager;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionExpiredRunnableTest {
    private final Instant fixedInstant = Instant.parse("2024-05-01T23:59:59.00Z");
    private final UUID permissionId = UUID.fromString("5f0b7a3c-6a43-4a43-9f2c-0b3e5a9f2d11");
    private final UUID dataNeedId = UUID.fromString("82831e2c-a01c-41b8-9db6-3f51670df7a5");
    private final Clock clock = Clock.fixed(fixedInstant, AiidaConfiguration.AIIDA_ZONE_ID);
    @Mock
    private AcceptedPermission mockAcceptedPermission;
    @Mock
    private Permission mockCurrentPermission;
    @Mock
    private StreamerManager streamerManager;
    @Mock
    private PermissionRepository repository;
    @Mock
    private AiidaLocalDataNeed mockAiidaDataNeed;

    @BeforeEach
    void setUp() {
        when(mockAcceptedPermission.id()).thenReturn(permissionId);
        when(mockAcceptedPermission.expirationTime()).thenReturn(fixedInstant);
    }

    @Test
    void verify_run_changesState_stopsStreamer_andUpdatesDb() {
        // Given
        when(mockAcceptedPermission.connectionId()).thenReturn("connectionId");
        when(mockAcceptedPermission.dataNeed()).thenReturn(mockAiidaDataNeed);
        when(mockAiidaDataNeed.dataNeedId()).thenReturn(dataNeedId);
        when(repository.findById(permissionId)).thenReturn(Optional.of(mockCurrentPermission));
        when(mockCurrentPermission.status()).thenReturn(PermissionStatus.ACCEPTED);
        var runnable = new PermissionExpiredRunnable(mockAcceptedPermission, streamerManager, repository, clock);

        // When
        runnable.run();

        // Then
        verify(streamerManager).stopStreamer(argThat(msg -> msg.status() == PermissionProcessStatus.FULFILLED));
        verify(mockCurrentPermission).setStatus(PermissionStatus.FULFILLED);
        verify(repository).save(mockCurrentPermission);
    }

    @Test
    void givenPermissionRevokedInTheMeantime_run_doesNotExpirePermission() {
        // Given
        when(repository.findById(permissionId)).thenReturn(Optional.of(mockCurrentPermission));
        when(mockCurrentPermission.status()).thenReturn(PermissionStatus.REVOKED);
        var runnable = new PermissionExpiredRunnable(mockAcceptedPermission, streamerManager, repository, clock);

        // When
        runnable.run();

        // Then
        verifyNoInteractions(streamerManager);
        verify(repository, never()).save(any());
        verify(mockCurrentPermission, never()).setStatus(any());
    }

    @Test
    void givenPermissionNoLongerExists_run_doesNotExpirePermission() {
        // Given
        when(repository.findById(permissionId)).thenReturn(Optional.empty());
        var runnable = new PermissionExpiredRunnable(mockAcceptedPermission, streamerManager, repository, clock);

        // When
        runnable.run();

        // Then
        verifyNoInteractions(streamerManager);
        verify(repository, never()).save(any());
    }
}
