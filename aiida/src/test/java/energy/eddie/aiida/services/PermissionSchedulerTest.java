// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services;

import energy.eddie.aiida.config.AiidaConfiguration;
import energy.eddie.aiida.models.permission.AcceptedPermission;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.streamers.StreamerManager;
import energy.eddie.aiida.utils.PermissionExpiredRunnable;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;

import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionSchedulerTest {
    private final Clock clock = Clock.fixed(Instant.parse("2023-10-10T10:00:00.00Z"), AiidaConfiguration.AIIDA_ZONE_ID);
    private final UUID eddieId = UUID.fromString("a69f9bc2-e16c-4de4-8c3e-00d219dcd819");
    private final UUID permissionId = UUID.fromString("b69f9bc2-e16c-4de4-8c3e-00d219dcd819");
    @Mock
    private AcceptedPermission permission;
    @Mock
    private TaskScheduler mockTaskScheduler;
    @Mock
    private PermissionRepository mockRepository;
    @Spy
    private ConcurrentMap<UUID, ScheduledFuture<?>> permissionFutures;
    @Mock
    private StreamerManager mockStreamerManager;
    @Mock
    private ScheduledFuture<?> mockScheduledFuture;
    private PermissionScheduler permissionScheduler;

    @BeforeEach
    void setUp() {
        lenient().when(permission.id()).thenReturn(permissionId);
        lenient().when(permission.eddieId()).thenReturn(eddieId);
        permissionScheduler = new PermissionScheduler(clock,
                                                      mockTaskScheduler,
                                                      mockRepository,
                                                      permissionFutures,
                                                      mockStreamerManager);
    }

    @Test
    void givenStartTimeInPast_startsStreamer_andSchedulesExpirationRunnable() throws MqttException {
        // Given
        var startTimeInPast = Instant.parse("2023-09-01T22:00:00.00Z");
        var expirationTime = clock.instant().plusSeconds(10000);
        when(permission.startTime()).thenReturn(startTimeInPast);
        when(permission.expirationTime()).thenReturn(expirationTime);

        // When
        permissionScheduler.scheduleOrStart(permission);

        // Then
        verify(permissionFutures).put(eq(permissionId), any());
        verify(mockTaskScheduler).schedule(any(PermissionExpiredRunnable.class), eq(expirationTime));
        verify(mockStreamerManager).createNewStreamer(any());
        verifyNoInteractions(mockRepository);
    }

    @Test
    void givenStartTimeInFuture_schedulesStart() {
        // Given
        var startTimeInFuture = clock.instant().plusSeconds(1000);
        when(permission.startTime()).thenReturn(startTimeInFuture);

        // When
        permissionScheduler.scheduleOrStart(permission);

        // Then
        verify(permissionFutures).put(eq(permissionId), any());
        verify(mockTaskScheduler).schedule(not(any(PermissionExpiredRunnable.class)), eq(startTimeInFuture));
        verifyNoInteractions(mockStreamerManager);
        verifyNoInteractions(mockRepository);
    }

    @Test
    void givenExceptionFromStreamer_stillSchedulesExpiration() throws MqttException {
        // Given
        var startTimeInFuture = clock.instant().minusSeconds(100);
        var expirationTime = startTimeInFuture.plusSeconds(500);
        when(permission.startTime()).thenReturn(startTimeInFuture);
        when(permission.expirationTime()).thenReturn(expirationTime);
        doThrow(new MqttException(999)).when(mockStreamerManager).createNewStreamer(any());

        // When
        permissionScheduler.scheduleOrStart(permission);

        // Then
        verify(mockTaskScheduler).schedule(any(PermissionExpiredRunnable.class), eq(expirationTime));
        verifyNoInteractions(mockRepository);
    }

    @Test
    void removePermission_cancelsFuture() {
        // Given
        doReturn(mockScheduledFuture).when(permissionFutures).remove(permissionId);

        // When
        permissionScheduler.removePermission(permissionId);

        // Then
        verify(mockScheduledFuture).cancel(true);
        verify(permissionFutures).remove(permissionId);
    }
}
