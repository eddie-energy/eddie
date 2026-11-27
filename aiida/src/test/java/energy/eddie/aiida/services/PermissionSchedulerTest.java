// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services;

import energy.eddie.aiida.config.AiidaConfiguration;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.permission.*;
import energy.eddie.aiida.models.permission.dataneed.OutboundAiidaLocalDataNeed;
import energy.eddie.aiida.repositories.FailedToSendRepository;
import energy.eddie.aiida.repositories.OutboundPermissionRepository;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.schemas.pmd.PermissionMarketDocumentFactory;
import energy.eddie.aiida.streamers.StreamerManager;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.cim.v1_12.rpmd.RequestPermissionEnvelope;
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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    @Mock
    private OutboundPermissionRepository mockOutboundPermissionRepository;
    @Mock
    private DataSourceService mockDataSourceService;
    @Mock
    private ProvisioningService mockProvisioningService;
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
                                                      mockFailedToSendRepository,
                                                      mockOutboundPermissionRepository,
                                                      mockDataSourceService,
                                                      mockProvisioningService);
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
        verify(mockTaskScheduler).schedule(any(Runnable.class), eq(expirationTime));
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
        verify(mockTaskScheduler).schedule(any(Runnable.class), eq(expirationTime));
        verifyNoInteractions(mockRepository);
    }

    @Test
    void stopPermission_cancelsFuture_andStopsStreamer() {
        // Given
        var outboundPermission = mock(OutboundPermission.class);
        var permissionMarketDocument = mock(RequestPermissionEnvelope.class);
        when(outboundPermission.id()).thenReturn(permissionId);
        doReturn(mockScheduledFuture).when(permissionFutures).remove(permissionId);
        when(mockPermissionMarketDocumentFactory.create(outboundPermission, PermissionProcessStatus.REVOKED))
                .thenReturn(permissionMarketDocument);

        // When
        permissionScheduler.stopPermission(outboundPermission, PermissionProcessStatus.REVOKED);

        // Then
        verify(mockScheduledFuture).cancel(true);
        verify(permissionFutures).remove(permissionId);
        verify(mockStreamerManager).stopStreamer(permissionMarketDocument);
        verifyNoInteractions(mockDataSourceService, mockProvisioningService);
    }

    @Test
    void givenInboundPermission_stopPermission_fulfillsForwardingOutboundPermissions_andRemovesInboundDataSource() {
        // Given
        var inboundPermission = mock(InboundPermission.class);
        var inboundDataSource = mock(InboundDataSource.class);
        var forwardingPermission = mock(OutboundPermission.class);
        var forwardingPermissionId = UUID.fromString("d69f9bc2-e16c-4de4-8c3e-00d219dcd819");
        var dataSourceId = UUID.fromString("c69f9bc2-e16c-4de4-8c3e-00d219dcd819");
        var forwardingPermissionMarketDocument = mock(RequestPermissionEnvelope.class);
        var inboundPermissionMarketDocument = mock(RequestPermissionEnvelope.class);
        when(inboundPermission.id()).thenReturn(permissionId);
        when(inboundPermission.dataSource()).thenReturn(inboundDataSource);
        when(inboundDataSource.id()).thenReturn(dataSourceId);
        when(forwardingPermission.id()).thenReturn(forwardingPermissionId);
        when(mockOutboundPermissionRepository.findByDataSourceIdAndStatus(dataSourceId, PermissionStatus.ACCEPTED))
                .thenReturn(List.of(forwardingPermission));
        when(mockPermissionMarketDocumentFactory.create(forwardingPermission, PermissionProcessStatus.FULFILLED))
                .thenReturn(forwardingPermissionMarketDocument);
        when(mockPermissionMarketDocumentFactory.create(inboundPermission,
                                                        PermissionProcessStatus.EXTERNALLY_TERMINATED))
                .thenReturn(inboundPermissionMarketDocument);

        // When
        permissionScheduler.stopPermission(inboundPermission, PermissionProcessStatus.EXTERNALLY_TERMINATED);

        // Then
        var inOrder = inOrder(forwardingPermission, mockRepository, mockStreamerManager, mockProvisioningService,
                              mockDataSourceService, inboundPermission);
        inOrder.verify(mockStreamerManager).stopStreamer(inboundPermissionMarketDocument);
        inOrder.verify(forwardingPermission).setStatus(PermissionStatus.FULFILLED);
        inOrder.verify(mockRepository).save(forwardingPermission);
        inOrder.verify(mockStreamerManager).stopStreamer(forwardingPermissionMarketDocument);
        inOrder.verify(mockProvisioningService).stopPublisher(dataSourceId);
        inOrder.verify(mockDataSourceService).deleteDataSource(dataSourceId);
        inOrder.verify(inboundPermission).setDataSource(null);
    }

    @Test
    void givenInboundPermissionWithoutDataSource_stopPermission_removesNothing() {
        // Given
        var inboundPermission = mock(InboundPermission.class);
        when(inboundPermission.id()).thenReturn(permissionId);

        // When
        permissionScheduler.stopPermission(inboundPermission, PermissionProcessStatus.REVOKED);

        // Then
        verifyNoInteractions(mockOutboundPermissionRepository, mockDataSourceService, mockProvisioningService);
    }

    @Test
    void givenAcceptedPermission_expiration_fulfillsPermission_stopsStreamer_andUpdatesDb() {
        // Given
        var expiration = scheduledExpiration();
        var currentPermission = mock(OutboundPermission.class);
        when(mockRepository.findById(permissionId)).thenReturn(Optional.of(currentPermission));
        when(currentPermission.status()).thenReturn(PermissionStatus.ACCEPTED);

        // When
        expiration.run();

        // Then
        verify(permissionFutures).remove(permissionId);
        verify(currentPermission).setStatus(PermissionStatus.FULFILLED);
        verify(mockPermissionMarketDocumentFactory).create(currentPermission, PermissionProcessStatus.FULFILLED);
        verify(mockStreamerManager).stopStreamer(any());
        verify(mockRepository).save(currentPermission);
        verify(currentPermission, never()).setDataSource(any());
        verifyNoInteractions(mockDataSourceService, mockProvisioningService);
    }

    @Test
    void givenInboundPermission_expiration_removesInboundDataSource() {
        // Given
        var expiration = scheduledExpiration();
        var inboundPermission = mock(InboundPermission.class);
        var inboundDataSource = mock(InboundDataSource.class);
        var dataSourceId = UUID.fromString("c69f9bc2-e16c-4de4-8c3e-00d219dcd819");
        when(mockRepository.findById(permissionId)).thenReturn(Optional.of(inboundPermission));
        when(inboundPermission.status()).thenReturn(PermissionStatus.ACCEPTED);
        when(inboundPermission.dataSource()).thenReturn(inboundDataSource);
        when(inboundDataSource.id()).thenReturn(dataSourceId);

        // When
        expiration.run();

        // Then
        var inOrder = inOrder(inboundPermission, mockDataSourceService, mockRepository);
        inOrder.verify(inboundPermission).setStatus(PermissionStatus.FULFILLED);
        inOrder.verify(mockDataSourceService).deleteDataSource(dataSourceId);
        inOrder.verify(inboundPermission).setDataSource(null);
        inOrder.verify(mockRepository).save(inboundPermission);
        verify(mockProvisioningService).stopPublisher(dataSourceId);
    }

    @Test
    void givenPermissionRevokedInTheMeantime_expiration_doesNotExpirePermission() {
        // Given
        var expiration = scheduledExpiration();
        var currentPermission = mock(OutboundPermission.class);
        when(mockRepository.findById(permissionId)).thenReturn(Optional.of(currentPermission));
        when(currentPermission.status()).thenReturn(PermissionStatus.REVOKED);

        // When
        expiration.run();

        // Then
        verify(mockStreamerManager, never()).stopStreamer(any());
        verify(mockRepository, never()).save(any());
        verify(currentPermission, never()).setStatus(any());
    }

    @Test
    void givenPermissionNoLongerExists_expiration_doesNotExpirePermission() {
        // Given
        var expiration = scheduledExpiration();
        when(mockRepository.findById(permissionId)).thenReturn(Optional.empty());

        // When
        expiration.run();

        // Then
        verify(mockStreamerManager, never()).stopStreamer(any());
        verify(mockRepository, never()).save(any());
    }

    /**
     * Starts the permission and returns its scheduled expiration.
     */
    private Runnable scheduledExpiration() {
        var expirationTime = clock.instant().plusSeconds(10000);
        when(permission.startTime()).thenReturn(clock.instant().minusSeconds(100));
        when(permission.expirationTime()).thenReturn(expirationTime);
        permissionScheduler.scheduleOrStart(permission);
        verify(mockTaskScheduler).schedule(runnableCaptor.capture(), eq(expirationTime));
        return runnableCaptor.getValue();
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
                                                       mockFailedToSendRepository,
                                                       mockOutboundPermissionRepository,
                                                       mockDataSourceService,
                                                       mockProvisioningService);
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
            assertEquals(mockDataSource, outboundPermission.dataSource());
            verifyNoInteractions(mockDataSourceService);
        }

        @Test
        void givenExpiredInboundPermission_removesInboundDataSource() {
            // Given
            doReturn(Instant.parse("2023-10-01T12:00:00.00Z")).when(mockClock).instant();
            var inboundPermission = mock(InboundPermission.class);
            var inboundDataSource = mock(InboundDataSource.class);
            var dataSourceId = UUID.fromString("c69f9bc2-e16c-4de4-8c3e-00d219dcd819");
            when(inboundPermission.status()).thenReturn(PermissionStatus.ACCEPTED);
            when(inboundPermission.dataSource()).thenReturn(inboundDataSource);
            when(inboundPermission.mqttStreamingConfig()).thenReturn(mockMqttStreamingConfig);
            when(inboundPermission.expirationTime()).thenReturn(Instant.parse("2023-09-30T00:00:00.000Z"));
            when(inboundDataSource.id()).thenReturn(dataSourceId);
            when(mockRepository.findByMqttStreamingConfigIsNotNullAndStatus(PermissionStatus.ACCEPTED))
                    .thenReturn(List.of(inboundPermission));

            // When
            startupScheduler.onApplicationEvent(mock(ContextRefreshedEvent.class));

            // Then
            verify(inboundPermission).setStatus(PermissionStatus.FULFILLED);
            verify(inboundPermission).setDataSource(null);
            verify(mockProvisioningService).stopPublisher(dataSourceId);
            verify(mockDataSourceService).deleteDataSource(dataSourceId);
        }

        @Test
        void givenPermissionNoLongerAccepted_isSkipped() {
            // Given
            doReturn(Instant.parse("2023-10-01T12:00:00.00Z")).when(mockClock).instant();
            var fulfilledPermission = mock(OutboundPermission.class);
            when(fulfilledPermission.status()).thenReturn(PermissionStatus.FULFILLED);
            when(mockRepository.findByMqttStreamingConfigIsNotNullAndStatus(PermissionStatus.ACCEPTED))
                    .thenReturn(List.of(fulfilledPermission));

            // When
            startupScheduler.onApplicationEvent(mock(ContextRefreshedEvent.class));

            // Then
            verify(fulfilledPermission, never()).setStatus(any());
            verifyNoInteractions(mockScheduler, mockStreamerManager, mockFailedToSendRepository);
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
