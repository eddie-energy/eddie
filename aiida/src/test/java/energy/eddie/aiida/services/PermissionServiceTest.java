package energy.eddie.aiida.services;

import energy.eddie.aiida.dtos.PermissionDto;
import energy.eddie.aiida.errors.ConnectionStatusMessageSendFailedException;
import energy.eddie.aiida.errors.InvalidPermissionRevocationException;
import energy.eddie.aiida.errors.PermissionNotFoundException;
import energy.eddie.aiida.models.permission.KafkaStreamingConfig;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.streamers.ConnectionStatusMessage;
import energy.eddie.aiida.streamers.StreamerManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.boot.task.TaskSchedulerBuilder;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {
    @Mock
    ScheduledFuture<?> mockScheduledFuture;
    @Spy
    Logger logger;
    @Mock
    private PermissionRepository repository;
    @Mock
    private Clock clock;
    @Mock
    private StreamerManager streamerManager;
    @Mock
    private TaskScheduler scheduler;
    @Spy
    private ConcurrentMap<String, ScheduledFuture<?>> expirationFutures = new ConcurrentHashMap<>();
    @InjectMocks
    private PermissionService service;
    private Permission permission;
    private Instant start;
    private Instant expiration;
    private Instant grant;
    private String serviceName;
    private String connectionId;
    private Set<String> codes;
    private String bootstrapServers;
    private String validDataTopic;
    private String validStatusTopic;
    private String validSubscribeTopic;
    private KafkaStreamingConfig streamingConfig;
    private String permissionId;

    @BeforeEach
    void setUp() {
        permissionId = "72831e2c-a01c-41b8-9db6-3f51670df7a5";
        grant = Instant.parse("2023-08-01T10:00:00.00Z");
        start = grant.plusSeconds(100_000);
        expiration = start.plusSeconds(800_000);
        serviceName = "My NewAIIDA Test Service";
        connectionId = "NewAiidaRandomConnectionId";
        codes = Set.of("1.8.0", "2.8.0");

        bootstrapServers = "localhost:9092";
        validDataTopic = "ValidPublishTopic";
        validStatusTopic = "ValidStatusTopic";
        validSubscribeTopic = "ValidSubscribeTopic";

        streamingConfig = new KafkaStreamingConfig(bootstrapServers, validDataTopic, validStatusTopic, validSubscribeTopic);
        permission = new Permission(serviceName, start, expiration, grant, connectionId, codes, streamingConfig);
    }

    @Test
    void givenNotExistingPermission_findById_throws() {
        var notExistingId = "NotExistingId";

        when(repository.findById(notExistingId)).thenReturn(Optional.empty());

        assertThrows(PermissionNotFoundException.class, () -> service.findById(notExistingId));
    }

    @Test
    void givenValidInput_setupPermission_asExpected() throws ConnectionStatusMessageSendFailedException {
        var permissionDto = new PermissionDto(serviceName, start, expiration, grant, connectionId, codes, streamingConfig);

        when(repository.save(any(Permission.class))).then(i -> {
            var arg = ((Permission) i.getArgument(0));
            ReflectionTestUtils.setField(arg, "permissionId", permissionId);
            return arg;
        });
        Mockito.doReturn(mockScheduledFuture).when(scheduler).schedule(Mockito.any(), Mockito.any(Instant.class));

        Permission newPermission = service.setupNewPermission(permissionDto);

        assertEquals(permissionId, newPermission.permissionId());
        assertEquals(serviceName, newPermission.serviceName());
        assertEquals(start, newPermission.startTime());
        assertEquals(expiration, newPermission.expirationTime());
        assertEquals(grant, newPermission.grantTime());
        assertEquals(connectionId, newPermission.connectionId());
        assertThat(codes).hasSameElementsAs(newPermission.requestedCodes());
        assertEquals(bootstrapServers, newPermission.kafkaStreamingConfig().bootstrapServers());
        assertEquals(validDataTopic, newPermission.kafkaStreamingConfig().dataTopic());
        assertEquals(validStatusTopic, newPermission.kafkaStreamingConfig().statusTopic());
        assertEquals(validSubscribeTopic, newPermission.kafkaStreamingConfig().subscribeTopic());
        assertEquals(PermissionStatus.STREAMING_DATA, newPermission.status());
        assertNull(newPermission.revokeTime());

        verify(repository, atLeastOnce()).save(any(Permission.class));
        verify(streamerManager).createNewStreamerForPermission(any(Permission.class));
        verify(streamerManager).sendConnectionStatusMessageForPermission(any(ConnectionStatusMessage.class), eq(permissionId));
        verify(scheduler).schedule(any(), eq(expiration));
    }

    /**
     * Tests that setting up a new permission schedules the expiration runnable correctly and that this
     * runnable runs and works as expected.
     */
    @Test
    void verify_expirePermission_sendsStatusMessage_andStopsStreamer_andUpdatesDb()
            throws ConnectionStatusMessageSendFailedException {
        start = Instant.now();
        expiration = start.plusMillis(500);
        var permissionDto = new PermissionDto(serviceName, start, expiration, start, connectionId, codes, streamingConfig);
        permission = new Permission(serviceName, start, expiration, start, connectionId, codes, streamingConfig);
        ReflectionTestUtils.setField(permission, "permissionId", permissionId);

        when(repository.save(any(Permission.class))).then(i -> {
            var arg = ((Permission) i.getArgument(0));
            ReflectionTestUtils.setField(arg, "permissionId", permissionId);
            return arg;
        });

        when(repository.findById(permissionId)).thenReturn(Optional.of(permission));

        when(scheduler.schedule(any(), any(Instant.class))).thenAnswer(i -> {
            ((Runnable) i.getArgument(0)).run();
            return mockScheduledFuture;
        });

        service.setupNewPermission(permissionDto);

        verify(streamerManager).createNewStreamerForPermission(any());
        verify(streamerManager).sendConnectionStatusMessageForPermission(
                argThat(arg -> arg.status() == PermissionStatus.ACCEPTED), eq(permissionId));
        verify(scheduler).schedule(any(), eq(expiration));

        verify(streamerManager).sendConnectionStatusMessageForPermission(
                argThat(arg -> arg.status() == PermissionStatus.TIME_LIMIT), eq(permissionId));
        verify(streamerManager).stopStreamer(permissionId);
        verify(repository).save(argThat(arg -> arg.status() == PermissionStatus.TIME_LIMIT));
    }

    @Test
    void givenNonExistingPermission_expirePermission_willLogError() throws ConnectionStatusMessageSendFailedException {
        var permissionId = "ba1c641e-df74-4bdf-8cd4-2ae2785c05a9";
        start = Instant.now();
        expiration = start.plusMillis(500);
        var permissionDto = new PermissionDto(serviceName, start, expiration, start, connectionId, codes, streamingConfig);

        when(repository.save(any(Permission.class))).then(i -> {
            var arg = ((Permission) i.getArgument(0));
            ReflectionTestUtils.setField(arg, "permissionId", permissionId);
            return arg;
        });
        when(repository.findById(permissionId)).thenReturn(Optional.empty());
        when(scheduler.schedule(any(), any(Instant.class))).thenAnswer(i -> {
            ((Runnable) i.getArgument(0)).run();
            return mockScheduledFuture;
        });

        service.setupNewPermission(permissionDto);

        verify(logger).info("Will expire permission with id {}", permissionId);
        verify(logger).error("No permission with id {} found in database, but was requested to expire it.", permissionId);
    }

    /**
     * Tests safety check, that expirePermission won't update a permission if it has been modified by another
     * part of AIIDA (e.g. revoked, but the cancellation of the runnable was not successful).
     */
    @Test
    void givenModifiedPermission_expirePermission_willLogError() throws ConnectionStatusMessageSendFailedException {
        var permissionId = "ba1c641e-df74-4bdf-8cd4-2ae2785c05a9";
        start = Instant.now();
        expiration = start.plusMillis(500);
        var permissionDto = new PermissionDto(serviceName, start, expiration, start, connectionId, codes, streamingConfig);
        permission = new Permission(serviceName, start, expiration, start, connectionId, codes, streamingConfig);
        ReflectionTestUtils.setField(permission, "permissionId", permissionId);
        ReflectionTestUtils.setField(permission, "status", PermissionStatus.REVOKED);

        when(repository.save(any(Permission.class))).thenReturn(permission);
        when(repository.findById(permissionId)).thenReturn(Optional.of(permission));
        when(scheduler.schedule(any(), any(Instant.class))).thenAnswer(i -> {
            ((Runnable) i.getArgument(0)).run();
            return mockScheduledFuture;
        });

        service.setupNewPermission(permissionDto);

        verify(logger).info("Will expire permission with id {}", permissionId);
        verify(logger).warn("Permission {} was modified, its status is {}. Will NOT expire the permission", permissionId, PermissionStatus.REVOKED);
    }

    @Test
    void givenExceptionWhenSendingConnectionStatusMessage_expirePermission_willLogError() throws ConnectionStatusMessageSendFailedException {
        var permissionId = "ba1c641e-df74-4bdf-8cd4-2ae2785c05a9";
        start = Instant.now();
        expiration = start.plusMillis(500);
        var permissionDto = new PermissionDto(serviceName, start, expiration, start, connectionId, codes, streamingConfig);
        permission = new Permission(serviceName, start, expiration, start, connectionId, codes, streamingConfig);
        ReflectionTestUtils.setField(permission, "permissionId", permissionId);
        ReflectionTestUtils.setField(permission, "status", PermissionStatus.STREAMING_DATA);

        when(repository.save(any(Permission.class))).thenReturn(permission);
        when(repository.findById(permissionId)).thenReturn(Optional.of(permission));
        when(scheduler.schedule(any(), any(Instant.class))).thenAnswer(i -> {
            ((Runnable) i.getArgument(0)).run();
            return mockScheduledFuture;
        });
        // during setupNewPermission, no error should be thrown
        doNothing().doThrow(ConnectionStatusMessageSendFailedException.class)
                .when(streamerManager).sendConnectionStatusMessageForPermission(any(), anyString());

        service.setupNewPermission(permissionDto);

        verify(logger).info("Will expire permission with id {}", permissionId);
        verify(logger).error(eq("Error while sending TIME_LIMIT ConnectionStatusMessage"), any(ConnectionStatusMessageSendFailedException.class));
    }

    @Nested
    @DisplayName("Test service layer revocation of a permission")
    class PermissionServiceRevocationTest {
        @Test
        void givenNotExistingPermissionId_revokePermission_throws() {
            var notExistingId = "NotExistingId";

            when(repository.findById(notExistingId)).thenReturn(Optional.empty());

            assertThrows(PermissionNotFoundException.class, () -> service.revokePermission(notExistingId));
        }

        @ParameterizedTest
        @EnumSource(
                value = PermissionStatus.class,
                names = {"ACCEPTED", "WAITING_FOR_START", "STREAMING_DATA"},
                mode = EnumSource.Mode.EXCLUDE)
        void givenPermissionWithInvalidStatus_revokePermission_throws(PermissionStatus status) {
            when(repository.findById(permissionId)).then(i -> {
                ReflectionTestUtils.setField(permission, "status", status);
                return Optional.of(permission);
            });

            assertThrows(InvalidPermissionRevocationException.class, () -> service.revokePermission(permissionId));
        }

        @ParameterizedTest
        @EnumSource(
                value = PermissionStatus.class,
                names = {"ACCEPTED", "WAITING_FOR_START", "STREAMING_DATA"},
                mode = EnumSource.Mode.INCLUDE)
        void givenValidPermission_revokePermission_asExpected(PermissionStatus status) throws ConnectionStatusMessageSendFailedException {
            Instant revokeTime = Instant.parse("2023-09-13T10:15:30.00Z");
            when(clock.instant()).thenReturn(revokeTime);

            ReflectionTestUtils.setField(permission, "permissionId", permissionId);
            ReflectionTestUtils.setField(permission, "status", status);

            when(repository.findById(permissionId)).thenReturn(Optional.of(permission));
            when(repository.save(any(Permission.class))).then(i -> i.getArgument(0));

            Permission revokedPermission = service.revokePermission(permissionId);

            // these fields must not have been modified
            assertEquals(permissionId, revokedPermission.permissionId());
            assertEquals(serviceName, revokedPermission.serviceName());
            assertEquals(start, revokedPermission.startTime());
            assertEquals(expiration, revokedPermission.expirationTime());
            assertEquals(grant, revokedPermission.grantTime());
            assertEquals(connectionId, revokedPermission.connectionId());

            assertThat(codes).hasSameElementsAs(revokedPermission.requestedCodes());

            assertEquals(bootstrapServers, revokedPermission.kafkaStreamingConfig().bootstrapServers());
            assertEquals(validDataTopic, revokedPermission.kafkaStreamingConfig().dataTopic());
            assertEquals(validStatusTopic, revokedPermission.kafkaStreamingConfig().statusTopic());
            assertEquals(validSubscribeTopic, revokedPermission.kafkaStreamingConfig().subscribeTopic());

            // this changed
            assertEquals(PermissionStatus.REVOKED, revokedPermission.status());
            assertEquals(revokeTime, revokedPermission.revokeTime());


            verify(repository, atLeastOnce()).findById(permissionId);
            verify(repository, atLeastOnce()).save(any(Permission.class));
            verify(streamerManager).stopStreamer(permissionId);
            verify(streamerManager, times(2)).sendConnectionStatusMessageForPermission(any(ConnectionStatusMessage.class), eq(permissionId));
        }

        @Test
        void givenRevokeTimeBeforeGrantTime_revokePermission_throws() {
            Instant grantTime = Instant.parse("2023-10-01T10:00:00.00Z");
            Instant revokeTime = Instant.parse("2023-09-01T10:00:00.00Z");
            when(clock.instant()).thenReturn(revokeTime);

            ReflectionTestUtils.setField(permission, "permissionId", permissionId);
            ReflectionTestUtils.setField(permission, "grantTime", grantTime);

            when(repository.findById(permissionId)).thenReturn(Optional.of(permission));

            assertThrows(IllegalArgumentException.class, () -> service.revokePermission(permissionId));
        }

        /**
         * Tests that a permission can be setup correctly and revoking it will cause the
         * {@link energy.eddie.aiida.utils.PermissionExpiredRunnable} to be cancelled and no TIME_LIMIT status message
         * will be sent.
         */
        @Test
        void verify_revokePermission_cancelsPermissionExpiredRunnable() throws ConnectionStatusMessageSendFailedException {
            // create a new PermissionService with a real scheduler
            var scheduler = new TaskSchedulerBuilder().awaitTermination(true).build();
            scheduler.initialize();
            var service = new PermissionService(repository, clock, streamerManager, scheduler, expirationFutures);

            start = Instant.now();
            expiration = start.plusMillis(500);
            var permissionDto = new PermissionDto(serviceName, start, expiration, start, connectionId, codes, streamingConfig);
            permission = new Permission(serviceName, start, expiration, start, connectionId, codes, streamingConfig);
            ReflectionTestUtils.setField(permission, "permissionId", permissionId);

            when(repository.save(any(Permission.class))).then(i -> {
                var arg = ((Permission) i.getArgument(0));
                ReflectionTestUtils.setField(arg, "permissionId", permissionId);
                return arg;
            });
            when(clock.instant()).thenReturn(Instant.now());
            when(repository.findById(permissionId)).thenReturn(Optional.of(permission));


            service.setupNewPermission(permissionDto);
            service.revokePermission(permissionId);

            // setup should have been executed correctly
            verify(streamerManager).createNewStreamerForPermission(any());
            verify(streamerManager).sendConnectionStatusMessageForPermission(
                    argThat(arg -> arg.status() == PermissionStatus.ACCEPTED), eq(permissionId));
            verify(expirationFutures).get(permissionId);

            // revoke should have been executed correctly
            verify(streamerManager, times(2)).sendConnectionStatusMessageForPermission(
                    argThat(arg -> arg.status() == PermissionStatus.REVOCATION_RECEIVED || arg.status() == PermissionStatus.REVOKED), eq(permissionId));


            // ensure the expiration runnable has not caused updates to the permission
            verify(streamerManager, never()).sendConnectionStatusMessageForPermission(
                    argThat(arg -> arg.status() == PermissionStatus.TIME_LIMIT), eq(permissionId));

            scheduler.shutdown();
        }

        @Test
        void givenExceptionWhenSendingConnectionStatusMessage_revokePermission_willLogError() throws ConnectionStatusMessageSendFailedException {
            Instant revokeTime = Instant.parse("2023-09-13T10:15:30.00Z");
            when(clock.instant()).thenReturn(revokeTime);

            ReflectionTestUtils.setField(permission, "permissionId", permissionId);
            ReflectionTestUtils.setField(permission, "status", PermissionStatus.STREAMING_DATA);

            when(repository.findById(permissionId)).thenReturn(Optional.of(permission));
            when(repository.save(any(Permission.class))).then(i -> i.getArgument(0));
            doThrow(ConnectionStatusMessageSendFailedException.class)
                    .when(streamerManager).sendConnectionStatusMessageForPermission(any(), eq(permissionId));

            service.revokePermission(permissionId);

            verify(logger).error(eq("Error while sending connection status messages while revoking permission {}"),
                    eq(permissionId), any(ConnectionStatusMessageSendFailedException.class));
        }

        @Test
        void givenNoStreamerForPermissionId_revokePermission_willLogError() throws ConnectionStatusMessageSendFailedException {
            Instant revokeTime = Instant.parse("2023-09-13T10:15:30.00Z");
            when(clock.instant()).thenReturn(revokeTime);

            ReflectionTestUtils.setField(permission, "permissionId", permissionId);
            ReflectionTestUtils.setField(permission, "status", PermissionStatus.STREAMING_DATA);

            when(repository.findById(permissionId)).thenReturn(Optional.of(permission));
            when(repository.save(any(Permission.class))).then(i -> i.getArgument(0));
            doThrow(IllegalArgumentException.class)
                    .when(streamerManager).sendConnectionStatusMessageForPermission(any(), eq(permissionId));

            service.revokePermission(permissionId);

            verify(logger).error(eq("Error while sending connection status messages while revoking permission {}"),
                    eq(permissionId), any(IllegalArgumentException.class));
        }
    }

    @Nested
    @DisplayName("Tests whether the @PostConstruct annotated method of PermissionService updates the permissions correctly")
    class PermissionServiceTestUpdatePermissionsOnStartup {
        @Mock
        PermissionRepository repository;
        @Mock
        StreamerManager streamerManager;
        @Mock
        Clock clock;
        @InjectMocks
        PermissionService service;

        /**
         * Tests that permissions are queried from the DB on startup and if their expiration time has passed,
         * their status is set accordingly or streaming is started again otherwise.
         * Mainly exists to cover the @PostConstruct method by using reflection, as test coverage is only
         * taken from unit tests not integration tests.
         * <p>
         * {@link PermissionServiceIntegrationTest} tests the same functionality but with a database and
         * ensures that the method is correctly configured to be called by Spring on startup.
         * </p>
         */
        @Test
        void givenVariousPermissions_statusAsExpected() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
            var codes = Set.of("1.8.0", "2.8.0");
            var streamingConfig = new KafkaStreamingConfig("localhost:9093", "foo", "bar", "tock");

            var start = Instant.parse("2023-09-01T00:00:00.000Z");
            var expiration = Instant.parse("2023-12-24T00:00:00.000Z");
            var acceptedShouldBeStreamingData = new Permission("Should be STREAMING_DATA after test completes", start,
                    expiration, start, "SomeConnectionId", codes, streamingConfig);
            String shouldStreamPermissionId = "25ee5365-5d71-4b01-b21f-9c61f76a5cc9";
            ReflectionTestUtils.setField(acceptedShouldBeStreamingData, "permissionId", shouldStreamPermissionId);

            start = Instant.parse("2023-09-01T00:00:00.000Z");
            expiration = Instant.parse("2023-09-19T00:00:00.000Z");
            var waitingForStartShouldBeTimeLimit = new Permission(
                    "Was WAITING_FOR_START and should be TIME_LIMIT after test completes", start,
                    expiration, start, "SomeConnectionId", codes, streamingConfig);
            String timeLimit1 = "9609a9b3-0718-4082-935d-6a98c0f8c5a2";
            ReflectionTestUtils.setField(waitingForStartShouldBeTimeLimit, "permissionId", timeLimit1);
            ReflectionTestUtils.setField(waitingForStartShouldBeTimeLimit, "status", PermissionStatus.WAITING_FOR_START);

            start = Instant.parse("2023-09-11T00:00:00.000Z");
            expiration = Instant.parse("2023-09-30T00:00:00.000Z");
            var streamingDataShouldBeTimeLimit = new Permission(
                    "Was STREAMING_DATA and should be TIME_LIMIT after test completes", start,
                    expiration, start, "SomeConnectionId", codes, streamingConfig);
            String timeLimit2 = "0b3b6f6d-d878-49dd-9dfd-62156b5cdc37";
            ReflectionTestUtils.setField(streamingDataShouldBeTimeLimit, "permissionId", timeLimit2);
            ReflectionTestUtils.setField(streamingDataShouldBeTimeLimit, "status", PermissionStatus.STREAMING_DATA);

            start = Instant.parse("2023-09-11T00:00:00.000Z");
            expiration = Instant.parse("2023-10-31T00:00:00.000Z");
            var streamingDataShouldBeStreamingData = new Permission(
                    "Was STREAMING_DATA and should be STREAMING_DATA after test completes", start,
                    expiration, start, "SomeConnectionId", codes, streamingConfig);
            String streamingDataId = "f53aa9e2-1969-4d86-a5e0-d76b2fd72863";
            ReflectionTestUtils.setField(streamingDataShouldBeStreamingData, "permissionId", streamingDataId);
            ReflectionTestUtils.setField(streamingDataShouldBeStreamingData, "status", PermissionStatus.STREAMING_DATA);

            when(clock.instant()).thenReturn(Instant.parse("2023-10-01T12:00:00.00Z"));
            when(repository.findAllActivePermissions()).thenReturn(List.of(acceptedShouldBeStreamingData,
                    waitingForStartShouldBeTimeLimit, streamingDataShouldBeTimeLimit, streamingDataShouldBeStreamingData));
            Map<String, Permission> savedByMethod = new HashMap<>();
            when(repository.save(any(Permission.class))).thenAnswer(i -> {
                var per = (Permission) i.getArgument(0);
                savedByMethod.put(per.permissionId(), per);
                return per;
            });


            getUpdatePermissionsOnStartup().invoke(service);

            var permission = savedByMethod.get(shouldStreamPermissionId);
            assertEquals(PermissionStatus.STREAMING_DATA, permission.status());
            verify(streamerManager).createNewStreamerForPermission(argThat(arg ->
                    arg.permissionId().equals(shouldStreamPermissionId)));

            permission = savedByMethod.get(timeLimit1);
            assertEquals(PermissionStatus.TIME_LIMIT, permission.status());

            permission = savedByMethod.get(timeLimit2);
            assertEquals(PermissionStatus.TIME_LIMIT, permission.status());

            permission = savedByMethod.get(streamingDataId);
            assertEquals(PermissionStatus.STREAMING_DATA, permission.status());
            verify(streamerManager).createNewStreamerForPermission(argThat(arg ->
                    arg.permissionId().equals(streamingDataId)));
        }

        private Method getUpdatePermissionsOnStartup() throws NoSuchMethodException {
            Method method = PermissionService.class.getDeclaredMethod("updatePermissionsOnStartup");
            method.setAccessible(true);
            return method;
        }
    }
}