package energy.eddie.aiida.services;

import energy.eddie.aiida.config.AiidaConfiguration;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.streamers.StreamerManager;
import energy.eddie.aiida.utils.PermissionExpiredRunnable;
import energy.eddie.api.agnostic.aiida.QrCodeDto;
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
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionSchedulerTest {
    @Mock
    private TaskScheduler mockTaskScheduler;
    @Mock
    private PermissionRepository mockRepository;
    @Spy
    private ConcurrentMap<String, ScheduledFuture<?>> permissionFutures;
    @Mock
    private StreamerManager mockStreamerManager;
    @Mock
    private ScheduledFuture<?> mockScheduledFuture;
    private PermissionScheduler permissionScheduler;
    private final Clock clock = Clock.fixed(Instant.parse("2023-10-10T10:00:00.00Z"), AiidaConfiguration.AIIDA_ZONE_ID);
    private final String permissionId = "fooId";
    private final String serviceName = "My Test Service";
    private final String handshakeUrl = "https://example.org";
    private final String accessToken = "fooBar";
    private final Permission permission = new Permission(new QrCodeDto(permissionId,
                                                                       serviceName,
                                                                       handshakeUrl,
                                                                       accessToken));

    @BeforeEach
    void setUp() {
        permissionScheduler = new PermissionScheduler(clock, mockTaskScheduler,
                                                      mockRepository, permissionFutures,
                                                      mockStreamerManager);
    }

    @Test
    void givenStartTimeInPast_changesState_andSchedulesExpirationRunnable() throws MqttException {
        // Given
        var startTimeInPast = Instant.parse("2023-09-01T22:00:00.00Z");
        var expirationTime = clock.instant().plusSeconds(10000);
        permission.setStartTime(startTimeInPast);
        permission.setExpirationTime(expirationTime);

        // When
        permissionScheduler.scheduleOrStart(permission);

        // Then
        assertEquals(PermissionStatus.STREAMING_DATA, permission.status());
        verify(permissionFutures).put(eq(permissionId), any());
        verify(mockTaskScheduler).schedule(any(PermissionExpiredRunnable.class), eq(expirationTime));
        verify(mockStreamerManager).createNewStreamerForPermission(any());
        verify(mockRepository).save(any());
    }

    @Test
    void givenStartTimeInFuture_changesState_andSchedulesStart() {
        // Given
        var startTimeInFuture = clock.instant().plusSeconds(1000);
        var expirationTime = startTimeInFuture.plusSeconds(5000);
        permission.setStartTime(startTimeInFuture);
        permission.setExpirationTime(expirationTime);

        // When
        permissionScheduler.scheduleOrStart(permission);

        // Then
        assertEquals(PermissionStatus.WAITING_FOR_START, permission.status());
        verify(permissionFutures).put(eq(permissionId), any());
        verify(mockTaskScheduler).schedule(not(any(PermissionExpiredRunnable.class)), eq(startTimeInFuture));
        verifyNoInteractions(mockStreamerManager);
        verify(mockRepository).save(any());
    }

    @Test
    void givenExceptionFromStreamer_updatesStatus() throws MqttException {
        // Given
        var startTimeInFuture = clock.instant().minusSeconds(100);
        var expirationTime = startTimeInFuture.plusSeconds(500);
        permission.setStartTime(startTimeInFuture);
        permission.setExpirationTime(expirationTime);
        doThrow(new MqttException(999)).when(mockStreamerManager).createNewStreamer(any());

        // When
        permissionScheduler.scheduleOrStart(permission);

        // Then
        assertEquals(PermissionStatus.FAILED_TO_START, permission.status());
        verify(mockRepository).save(any());
        verifyNoInteractions(mockTaskScheduler);
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
