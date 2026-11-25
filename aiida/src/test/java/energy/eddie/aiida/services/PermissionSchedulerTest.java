// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services;

import energy.eddie.aiida.config.AiidaConfiguration;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.permission.AcceptedOutboundPermission;
import energy.eddie.aiida.models.permission.MqttStreamingConfig;
import energy.eddie.aiida.models.permission.OutboundPermission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import energy.eddie.aiida.models.permission.dataneed.OutboundAiidaLocalDataNeed;
import energy.eddie.aiida.repositories.FailedToSendRepository;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.schemas.pmd.PermissionMarketDocumentFactory;
import energy.eddie.aiida.streamers.StreamerManager;
import energy.eddie.aiida.utils.PermissionExpiredRunnable;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronExpression;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionSchedulerTest {
    private final Clock clock = Clock.fixed(Instant.parse("2023-10-10T10:00:00.00Z"), AiidaConfiguration.AIIDA_ZONE_ID);
    private final UUID eddieId = UUID.fromString("a69f9bc2-e16c-4de4-8c3e-00d219dcd819");
    private final UUID permissionId = UUID.fromString("b69f9bc2-e16c-4de4-8c3e-00d219dcd819");
    @Mock
    private AcceptedOutboundPermission permission;
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
    @Mock
    private PermissionMarketDocumentFactory mockPermissionMarketDocumentFactory;
    @Mock
    private FailedToSendRepository mockFailedToSendRepository;
    @Captor
    private ArgumentCaptor<Runnable> runnableCaptor;
    private PermissionScheduler permissionScheduler;

    @BeforeEach
    void setUp() {
        lenient().when(permission.id()).thenReturn(permissionId);
        lenient().when(permission.eddieId()).thenReturn(eddieId);
        permissionScheduler = new PermissionScheduler(clock,
                                                      mockTaskScheduler,
                                                      mockRepository,
                                                      permissionFutures,
                                                      mockStreamerManager,
                                                      mockPermissionMarketDocumentFactory,
                                                      mockFailedToSendRepository);
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
        verify(mockTaskScheduler).schedule(runnableCaptor.capture(), eq(startTimeInFuture));
        assertFalse(runnableCaptor.getValue() instanceof PermissionExpiredRunnable);
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

    @Nested
    @DisplayName("Tests whether the ContextRefreshedEvent listener of PermissionScheduler updates the permissions correctly")
    class UpdatePermissionsOnStartup {
        @Mock
        private Clock mockClock;
        @Mock
        private TaskScheduler mockScheduler;
        @Mock
        private ScheduledFuture<?> mockScheduledFutureNested;
        @Mock
        private OutboundAiidaLocalDataNeed mockStartupDataNeed;
        @Mock
        private MqttStreamingConfig mockMqttStreamingConfig;
        @Mock
        private DataSource mockDataSource;
        private PermissionScheduler startupScheduler;

        public static Stream<Arguments> onStartupVariousPermissions() {
            return Stream.of(Arguments.of(
                                          "2023-09-01T00:00:00.000Z",
                                          "2023-12-24T00:00:00.000Z",
                                          PermissionStatus.ACCEPTED),
                             Arguments.of(
                                          "2023-09-01T00:00:00.000Z",
                                          "2023-09-19T00:00:00.000Z",
                                          PermissionStatus.FULFILLED),
                             Arguments.of(
                                          "2023-09-11T00:00:00.000Z",
                                          "2023-09-30T00:00:00.000Z",
                                          PermissionStatus.FULFILLED),
                             Arguments.of(
                                          "2023-09-11T00:00:00.000Z",
                                          "2023-10-31T00:00:00.000Z",
                                          PermissionStatus.ACCEPTED),
                             Arguments.of(
                                          "2023-10-12T00:00:00.000Z",
                                          "2023-10-15T00:00:00.000Z",
                                          PermissionStatus.ACCEPTED));
        }

        @BeforeEach
        void setUp() {
            startupScheduler = new PermissionScheduler(mockClock,
                                                       mockScheduler,
                                                       mockRepository,
                                                       new ConcurrentHashMap<>(),
                                                       mockStreamerManager,
                                                       mockPermissionMarketDocumentFactory,
                                                       mockFailedToSendRepository);
        }

        /**
         * Tests that permissions are queried from the DB on startup and if their expiration time has passed, their
         * status is set accordingly or streaming is started again otherwise.
         * <p>
         * {@link PermissionSchedulerIntegrationTest} tests the same functionality but with a database and ensures that
         * the method is correctly called by Spring on startup.
         * </p>
         */
        @ParameterizedTest
        @MethodSource("onStartupVariousPermissions")
        void givenVariousPermissions_statusAsExpected(
                String start,
                String end,
                PermissionStatus expectedState
        ) {
            // Given
            doReturn(Instant.parse("2023-10-01T12:00:00.00Z")).when(mockClock).instant();
            var outboundPermission = new OutboundPermission(UUID.fromString("15ee5365-5d71-4b01-b21f-9c61f76a5cc9"),
                                                    UUID.fromString("25ee5365-5d71-4b01-b21f-9c61f76a5cc9"),
                                                    "https://example.org",
                                                    "accessToken",
                                                    UUID.randomUUID(),
                                                    mockStartupDataNeed,
                                                    "connectionId",
                                                    Instant.parse(start),
                                                    Instant.parse(end),
                                                    Instant.parse("2026-01-01T00:00:00Z"));
            outboundPermission.setStatus(PermissionStatus.ACCEPTED);
            lenient().when(mockStartupDataNeed.transmissionSchedule()).thenReturn(CronExpression.parse("*/5 * * * * *"));
            outboundPermission.setDataSource(mockDataSource);
            outboundPermission.setMqttStreamingConfig(mockMqttStreamingConfig);
            when(mockRepository.findByMqttStreamingConfigIsNotNullAndStatus(PermissionStatus.ACCEPTED)).thenReturn(List.of(outboundPermission));

            if (expectedState == PermissionStatus.ACCEPTED) {
                doReturn(mockScheduledFutureNested).when(mockScheduler).schedule(any(), any(Instant.class));
            }

            // When
            startupScheduler.onApplicationEvent(mock(ContextRefreshedEvent.class));

            // Then
            assertEquals(expectedState, outboundPermission.status());
            if (expectedState == PermissionStatus.FULFILLED) {
                verify(mockFailedToSendRepository).deleteAllByPermissionId(outboundPermission.id());
            } else {
                verify(mockFailedToSendRepository, never()).deleteAllByPermissionId(any());
            }
        }

        @Test
        void givenIncompletePermission_isSkipped_andOtherPermissionsAreStillResumed() {
            // Given
            doReturn(Instant.parse("2023-10-01T12:00:00.00Z")).when(mockClock).instant();
            var incomplete = new OutboundPermission(UUID.randomUUID(),
                                                    UUID.randomUUID(),
                                                    "handshakeUrl",
                                                    "token",
                                                    UUID.randomUUID(),
                                                    mockStartupDataNeed,
                                                    "connection-id",
                                                    Instant.parse("2026-01-01T00:00:00Z"),
                                                    Instant.parse("2026-12-31T00:00:00Z"),
                                                    Instant.parse("2026-01-01T00:00:00Z"));
            incomplete.setStatus(PermissionStatus.ACCEPTED);
            var complete = new OutboundPermission(UUID.randomUUID(),
                                                  UUID.randomUUID(),
                                                  "handshakeUrl",
                                                  "token",
                                                  UUID.randomUUID(),
                                                  mockStartupDataNeed,
                                                  "connectionId",
                                                  Instant.parse("2023-10-02T00:00:00.000Z"),
                                                  Instant.parse("2023-10-05T00:00:00.000Z"),
                                                  Instant.parse("2026-01-01T00:00:00Z"));
            complete.setStatus(PermissionStatus.ACCEPTED);
            when(mockStartupDataNeed.transmissionSchedule()).thenReturn(CronExpression.parse("*/5 * * * * *"));
            complete.setDataSource(mockDataSource);
            complete.setMqttStreamingConfig(mockMqttStreamingConfig);
            when(mockRepository.findByMqttStreamingConfigIsNotNullAndStatus(PermissionStatus.ACCEPTED))
                    .thenReturn(List.of(incomplete, complete));
            doReturn(mockScheduledFutureNested).when(mockScheduler).schedule(any(), any(Instant.class));

            // When
            startupScheduler.onApplicationEvent(mock(ContextRefreshedEvent.class));

            // Then
            assertEquals(PermissionStatus.ACCEPTED, incomplete.status());
            assertEquals(PermissionStatus.ACCEPTED, complete.status());
            verify(mockScheduler).schedule(any(), any(Instant.class));
        }
    }
}
