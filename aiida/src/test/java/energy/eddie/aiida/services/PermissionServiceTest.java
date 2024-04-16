package energy.eddie.aiida.services;

import energy.eddie.aiida.dtos.PermissionDto;
import energy.eddie.aiida.errors.*;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.streamers.ConnectionStatusMessage;
import energy.eddie.aiida.streamers.StreamerManager;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.task.TaskSchedulerBuilder;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.test.publisher.TestPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;

import static energy.eddie.aiida.TestUtils.verifyErrorLogStartsWith;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {
    private static final LogCaptor logCaptor = LogCaptor.forClass(PermissionService.class);
    @Mock
    ScheduledFuture<?> mockScheduledFuture;
    @Mock
    private PermissionRepository repository;
    @Mock
    private StreamerManager streamerManager;
    @Mock
    private TaskScheduler scheduler;
    @Spy
    private ConcurrentMap<String, ScheduledFuture<?>> permissionFutures = new ConcurrentHashMap<>();
    private Permission permission;
    private TestPublisher<String> testPublisher;
    private PermissionService service;
    private String permissionId;
    private Instant start;
    private Instant expiration;
    private Instant grant;
    private String serviceName;
    private String connectionId;
    private String dataNeedId;
    private Set<String> codes;
    private Instant terminationTime;
    private Clock clock;

    @BeforeEach
    void setUp() {
        permissionId = "72831e2c-a01c-41b8-9db6-3f51670df7a5";
        grant = Instant.parse("2023-08-01T10:00:00.00Z");
        start = grant.plusSeconds(100_000);
        expiration = start.plusSeconds(800_000);
        serviceName = "My NewAIIDA Test Service";
        connectionId = "NewAiidaRandomConnectionId";
        dataNeedId = "dataNeedId";
        codes = Set.of("1.8.0", "2.8.0");

        permission = new Permission(permissionId,
                                    serviceName,
                                    dataNeedId,
                                    start,
                                    expiration,
                                    grant,
                                    connectionId,
                                    codes);

        testPublisher = TestPublisher.create();
        terminationTime = start.plusSeconds(200_000);
        clock = Clock.fixed(terminationTime, ZoneId.systemDefault());

        doReturn(testPublisher.flux()).when(streamerManager).terminationRequestsFlux();
        service = new PermissionService(repository, clock, streamerManager, scheduler, permissionFutures);
    }

    @AfterAll
    public static void afterAll() {
        logCaptor.close();
    }

    @AfterEach
    void tearDown() {
        logCaptor.clearLogs();
    }

    @Test
    void givenNotExistingPermission_findById_throws() {
        var notExistingId = "NotExistingId";

        when(repository.findById(notExistingId)).thenReturn(Optional.empty());

        assertThrows(PermissionNotFoundException.class, () -> service.findById(notExistingId));
    }

    @Test
    void givenValidInput_setupPermission_asExpected() throws PermissionStartFailedException, ConnectionStatusMessageSendFailedException, PermissionAlreadyExistsException {
        var permissionDto = new PermissionDto(permissionId,
                                              serviceName,
                                              dataNeedId,
                                              start,
                                              expiration,
                                              grant,
                                              connectionId,
                                              codes);

        when(repository.save(any(Permission.class))).then(i -> i.getArgument(0));
        Mockito.doReturn(mockScheduledFuture).when(scheduler).schedule(Mockito.any(), Mockito.any(Instant.class));

        Permission newPermission = service.setupNewPermission(permissionDto);

        assertEquals(permissionId, newPermission.permissionId());
        assertEquals(serviceName, newPermission.serviceName());
        assertEquals(start, newPermission.startTime());
        assertEquals(expiration, newPermission.expirationTime());
        assertEquals(grant, newPermission.grantTime());
        assertEquals(connectionId, newPermission.connectionId());
        assertEquals(dataNeedId, newPermission.dataNeedId());
        assertThat(codes).hasSameElementsAs(newPermission.requestedCodes());
        assertEquals(PermissionStatus.STREAMING_DATA, newPermission.status());
        assertNull(newPermission.revokeTime());

        verify(repository, atLeastOnce()).save(any(Permission.class));
        verify(streamerManager).createNewStreamerForPermission(any(Permission.class));
        verify(streamerManager).sendConnectionStatusMessageForPermission(any(ConnectionStatusMessage.class),
                                                                         eq(permissionId));
        verify(scheduler).schedule(any(), eq(expiration));
    }

    @Test
    void givenExceptionWhenSendingConnectionStatusMessage_setupPermission_changesStatus() throws ConnectionStatusMessageSendFailedException {
        var permissionDto = new PermissionDto(permissionId,
                                              serviceName,
                                              dataNeedId,
                                              start,
                                              expiration,
                                              grant,
                                              connectionId,
                                              codes);

        when(repository.save(any(Permission.class))).then(i -> i.getArgument(0));

        doThrow(ConnectionStatusMessageSendFailedException.class)
                .when(streamerManager).sendConnectionStatusMessageForPermission(any(), anyString());

        // When
        var thrown = assertThrows(PermissionStartFailedException.class,
                                  () -> service.setupNewPermission(permissionDto));

        // Then
        Permission permission = thrown.permission();
        assertEquals(PermissionStatus.FAILED_TO_START, permission.status());

        verify(repository, atLeastOnce()).save(argThat(per -> per.status() == PermissionStatus.FAILED_TO_START));
    }

    @Test
    void givenStartTimeInFuture_setupPermission_schedulesStart() throws PermissionStartFailedException, PermissionAlreadyExistsException {
        // Given
        start = Instant.now(clock).plusSeconds(1000);
        expiration = start.plusSeconds(2000);
        var permissionDto = new PermissionDto(permissionId,
                                              serviceName,
                                              dataNeedId,
                                              start,
                                              expiration,
                                              grant,
                                              connectionId,
                                              codes);
        when(repository.save(any(Permission.class))).then(i -> i.getArgument(0));
        Mockito.doReturn(mockScheduledFuture).when(scheduler).schedule(Mockito.any(), Mockito.any(Instant.class));

        // When
        var permission = service.setupNewPermission(permissionDto);

        // Then
        assertEquals(PermissionStatus.WAITING_FOR_START, permission.status());
        verify(repository, atLeastOnce()).save(argThat(per -> PermissionStatus.WAITING_FOR_START.equals(per.status())));
        verify(scheduler).schedule(any(), eq(start));
    }

    @Test
    void givenExistingPermissionId_setupPermission_throws() {
        // Given
        var permissionDto = new PermissionDto(permissionId,
                                              serviceName,
                                              dataNeedId,
                                              start,
                                              expiration,
                                              grant,
                                              connectionId,
                                              codes);
        when(repository.existsById(permissionId)).thenReturn(true);

        // When, Then
        assertThrows(PermissionAlreadyExistsException.class, () -> service.setupNewPermission(permissionDto));
    }

    /**
     * Tests that setting up a new permission schedules the expiration runnable correctly and that this runnable runs
     * and works as expected.
     */
    @Test
    void verify_expirePermission_sendsStatusMessage_andStopsStreamer_andUpdatesDb()
            throws ConnectionStatusMessageSendFailedException, PermissionStartFailedException, PermissionAlreadyExistsException {
        // Given
        start = Instant.now();
        expiration = start.plusMillis(500);
        var permissionDto = new PermissionDto(permissionId,
                                              serviceName,
                                              dataNeedId,
                                              start,
                                              expiration,
                                              start,
                                              connectionId,
                                              codes);
        permission = new Permission(permissionId,
                                    serviceName,
                                    dataNeedId,
                                    start,
                                    expiration,
                                    start,
                                    connectionId,
                                    codes);

        when(repository.save(any(Permission.class))).then(i -> i.getArgument(0));
        when(repository.findById(permissionId)).thenReturn(Optional.of(permission));

        when(scheduler.schedule(any(), any(Instant.class))).thenAnswer(i -> {
            ((Runnable) i.getArgument(0)).run();
            return mockScheduledFuture;
        });

        // When
        service.setupNewPermission(permissionDto);

        // Then
        verify(streamerManager).createNewStreamerForPermission(any());
        verify(streamerManager).sendConnectionStatusMessageForPermission(
                argThat(arg -> arg.status() == PermissionStatus.ACCEPTED), eq(permissionId));
        verify(scheduler).schedule(any(), eq(expiration));

        verify(streamerManager).sendConnectionStatusMessageForPermission(
                argThat(arg -> arg.status() == PermissionStatus.FULFILLED), eq(permissionId));
        verify(streamerManager).stopStreamer(permissionId);
        verify(repository).save(argThat(arg -> arg.status() == PermissionStatus.FULFILLED));
    }

    @Test
    void givenNonExistingPermission_expirePermission_willLogError() throws PermissionStartFailedException, PermissionAlreadyExistsException {
        // Given
        var permissionId = "ba1c641e-df74-4bdf-8cd4-2ae2785c05a9";
        start = Instant.now();
        expiration = start.plusMillis(500);
        var permissionDto = new PermissionDto(permissionId,
                                              serviceName,
                                              dataNeedId,
                                              start,
                                              expiration,
                                              start,
                                              connectionId,
                                              codes);

        when(repository.save(any(Permission.class))).then(i -> i.getArgument(0));
        when(repository.findById(permissionId)).thenReturn(Optional.empty());
        when(scheduler.schedule(any(), any(Instant.class))).thenAnswer(i -> {
            ((Runnable) i.getArgument(0)).run();
            return mockScheduledFuture;
        });

        // When
        service.setupNewPermission(permissionDto);

        // Then
        assertThat(logCaptor.getInfoLogs()).contains("Will expire permission with id %s".formatted(permissionId));
        assertThat(logCaptor.getErrorLogs()).contains(
                "No permission with id %s found in database, but was requested to expire it."
                        .formatted(permissionId));
    }

    /**
     * Tests safety check, that expirePermission won't update a permission if it has been modified by another part of
     * AIIDA (e.g. revoked, but the cancellation of the runnable was not successful).
     */
    @Test
    void givenModifiedPermission_expirePermission_willLogError() throws PermissionStartFailedException, PermissionAlreadyExistsException {
        var permissionId = "ba1c641e-df74-4bdf-8cd4-2ae2785c05a9";
        expiration = start.plusMillis(500);
        var permissionDto = new PermissionDto(permissionId,
                                              serviceName,
                                              dataNeedId,
                                              start,
                                              expiration,
                                              start,
                                              connectionId,
                                              codes);
        permission = new Permission(permissionId,
                                    serviceName,
                                    dataNeedId,
                                    start,
                                    expiration,
                                    start,
                                    connectionId,
                                    codes);
        permission.updateStatus(PermissionStatus.REVOKED);

        when(repository.save(any(Permission.class))).thenReturn(permission);
        when(repository.findById(permissionId)).thenReturn(Optional.of(permission));
        when(scheduler.schedule(any(), any(Instant.class))).thenAnswer(i -> {
            ((Runnable) i.getArgument(0)).run();
            return mockScheduledFuture;
        });

        service.setupNewPermission(permissionDto);

        assertThat(logCaptor.getInfoLogs()).contains("Will expire permission with id %s".formatted(permissionId));
        assertThat(logCaptor.getWarnLogs()).contains(
                "Permission %s was modified, its status is %s. Will NOT expire the permission"
                        .formatted(permissionId, PermissionStatus.REVOKED));
    }

    @Test
    void givenExceptionWhenSendingConnectionStatusMessage_expirePermission_willLogError() throws ConnectionStatusMessageSendFailedException, PermissionStartFailedException, PermissionAlreadyExistsException {
        var permissionId = "ba1c641e-df74-4bdf-8cd4-2ae2785c05a9";
        start = Instant.now();
        expiration = start.plusMillis(500);
        var permissionDto = new PermissionDto(permissionId,
                                              serviceName,
                                              dataNeedId,
                                              start,
                                              expiration,
                                              start,
                                              connectionId,
                                              codes);
        permission = new Permission(permissionId,
                                    serviceName,
                                    dataNeedId,
                                    start,
                                    expiration,
                                    start,
                                    connectionId,
                                    codes);
        permission.updateStatus(PermissionStatus.STREAMING_DATA);

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

        assertThat(logCaptor.getInfoLogs()).contains("Will expire permission with id %s".formatted(permissionId));
        verifyErrorLogStartsWith("Error while sending FULFILLED ConnectionStatusMessage", logCaptor,
                                 ConnectionStatusMessageSendFailedException.class);
    }

    @Test
    void givenTerminationRequest_sendsStatusMessages_andStopsStreaming_andUpdatesDB() throws ConnectionStatusMessageSendFailedException {
        when(repository.findById(permission.permissionId())).thenReturn(Optional.of(permission));

        testPublisher.next(permission.permissionId());

        verify(streamerManager).terminationRequestsFlux();
        verify(streamerManager).sendConnectionStatusMessageForPermission(
                argThat(arg -> arg.status() == PermissionStatus.TERMINATED), eq(permission.permissionId()));
        verify(streamerManager).stopStreamer(permission.permissionId());

        verify(repository).save(argThat(permission -> permission.status() == PermissionStatus.TERMINATED
                && Objects.requireNonNull(permission.revokeTime()).toEpochMilli() == terminationTime.toEpochMilli()));
    }

    @Test
    void givenTerminationRequest_whenPermissionWaitingForStart_doesNotSendStatusMessage() throws ConnectionStatusMessageSendFailedException {
        // Given
        start = Instant.now(clock).plusSeconds(1000);
        grant = Instant.now(clock).minusSeconds(150);
        expiration = start.plusSeconds(2000);
        permission = new Permission(permissionId,
                                    serviceName,
                                    dataNeedId,
                                    start,
                                    expiration,
                                    grant,
                                    connectionId,
                                    codes);
        permission.updateStatus(PermissionStatus.WAITING_FOR_START);
        when(repository.findById(permission.permissionId())).thenReturn(Optional.of(permission));
        doReturn(mockScheduledFuture).when(permissionFutures).get(permissionId);

        // When
        testPublisher.next(permission.permissionId());

        // Then
        verify(streamerManager, never()).stopStreamer(anyString());
        verify(streamerManager, never()).sendConnectionStatusMessageForPermission(any(), anyString());
        verify(permissionFutures).get(permissionId);
        verify(mockScheduledFuture).cancel(true);
        verify(repository, atLeastOnce()).findById(permissionId);
        verify(repository).save(argThat(per -> per.status() == PermissionStatus.TERMINATED));
    }

    @Nested
    @DisplayName("Test service layer revocation of a permission")
    class PermissionServiceRevocationTest {
        @BeforeEach
        void setUp() {
            clock = mock(Clock.class);
            service = new PermissionService(repository, clock, streamerManager, scheduler, permissionFutures);
        }

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
                names = {"ACCEPTED", "STREAMING_DATA"},
                // WAITING_FOR_START is tested by #givenRevoke_whenPermissionWaitingForStart_doesNotSendStatusMessage()
                mode = EnumSource.Mode.INCLUDE)
        void givenValidPermission_revokePermission_asExpected(PermissionStatus status) throws ConnectionStatusMessageSendFailedException {
            Instant revokeTime = Instant.parse("2023-09-13T10:15:30.00Z");
            doReturn(revokeTime).when(clock).instant();
            permission.updateStatus(status);

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

            // this changed
            assertEquals(PermissionStatus.REVOKED, revokedPermission.status());
            assertEquals(revokeTime, revokedPermission.revokeTime());


            verify(repository, atLeastOnce()).findById(permissionId);
            verify(repository, atLeastOnce()).save(any(Permission.class));
            verify(streamerManager).stopStreamer(permissionId);
            verify(streamerManager,
                   times(2)).sendConnectionStatusMessageForPermission(any(ConnectionStatusMessage.class),
                                                                      eq(permissionId));
        }

        @Test
        void givenRevokeTimeBeforeGrantTime_revokePermission_throws() {
            Instant grantTime = Instant.parse("2023-10-01T10:00:00.00Z");
            Instant revokeTime = Instant.parse("2023-09-01T10:00:00.00Z");
            doReturn(revokeTime).when(clock).instant();

            ReflectionTestUtils.setField(permission, "grantTime", grantTime);

            when(repository.findById(permissionId)).thenReturn(Optional.of(permission));

            assertThrows(IllegalArgumentException.class, () -> service.revokePermission(permissionId));
        }

        /**
         * Tests that a permission can be setup correctly and revoking it will cause the
         * {@link energy.eddie.aiida.utils.PermissionExpiredRunnable} to be cancelled and no FULFILLED status message
         * will be sent.
         */
        @Test
        void verify_revokePermission_cancelsPermissionExpiredRunnable() throws PermissionStartFailedException, ConnectionStatusMessageSendFailedException, PermissionAlreadyExistsException {
            // create a new PermissionService with a real scheduler
            var scheduler = new TaskSchedulerBuilder().awaitTermination(true).build();
            scheduler.initialize();
            var service = new PermissionService(repository, clock, streamerManager, scheduler, permissionFutures);

            start = Instant.now();
            expiration = start.plusMillis(500);
            var permissionDto = new PermissionDto(permissionId,
                                                  serviceName,
                                                  dataNeedId,
                                                  start,
                                                  expiration,
                                                  start,
                                                  connectionId,
                                                  codes);
            permission = new Permission(permissionId,
                                        serviceName,
                                        dataNeedId,
                                        start,
                                        expiration,
                                        start,
                                        connectionId,
                                        codes);

            when(repository.save(any(Permission.class))).then(i -> i.getArgument(0));
            when(clock.instant()).thenReturn(Instant.now());
            when(repository.findById(permissionId)).thenReturn(Optional.of(permission));


            service.setupNewPermission(permissionDto);
            service.revokePermission(permissionId);

            // setup should have been executed correctly
            verify(streamerManager).createNewStreamerForPermission(any());
            verify(streamerManager).sendConnectionStatusMessageForPermission(
                    argThat(arg -> arg.status() == PermissionStatus.ACCEPTED), eq(permissionId));
            verify(permissionFutures).get(permissionId);

            // revoke should have been executed correctly
            verify(streamerManager, times(2)).sendConnectionStatusMessageForPermission(
                    argThat(arg -> arg.status() == PermissionStatus.REVOCATION_RECEIVED || arg.status() == PermissionStatus.REVOKED),
                    eq(permissionId));


            // ensure the expiration runnable has not caused updates to the permission
            verify(streamerManager, never()).sendConnectionStatusMessageForPermission(
                    argThat(arg -> arg.status() == PermissionStatus.FULFILLED), eq(permissionId));

            scheduler.shutdown();
        }

        @Test
        void givenRevoke_whenPermissionWaitingForStart_doesNotSendStatusMessage() throws ConnectionStatusMessageSendFailedException {
            // Given
            clock = Clock.fixed(terminationTime, ZoneId.systemDefault());
            service = new PermissionService(repository, clock, streamerManager, scheduler, permissionFutures);
            start = Instant.now(clock).plusSeconds(1000);
            grant = Instant.now(clock).minusSeconds(150);
            expiration = start.plusSeconds(2000);
            permission = new Permission(permissionId,
                                        serviceName,
                                        dataNeedId,
                                        start,
                                        expiration,
                                        grant,
                                        connectionId,
                                        codes);
            permission.updateStatus(PermissionStatus.WAITING_FOR_START);
            when(repository.findById(permission.permissionId())).thenReturn(Optional.of(permission));
            when(repository.save(any(Permission.class))).then(i -> i.getArgument(0));
            doReturn(mockScheduledFuture).when(permissionFutures).get(permissionId);

            // When
            var permission = service.revokePermission(permissionId);

            // Then
            assertEquals(PermissionStatus.REVOKED, permission.status());
            verify(streamerManager, never()).stopStreamer(anyString());
            verify(streamerManager, never()).sendConnectionStatusMessageForPermission(any(), anyString());
            verify(permissionFutures).get(permissionId);
            verify(mockScheduledFuture).cancel(true);
            verify(repository, atLeastOnce()).findById(permissionId);
            verify(repository).save(argThat(per -> per.status() == PermissionStatus.REVOKED));
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

            assertThat(logCaptor.getErrorLogs())
                    .contains("Error while sending connection status messages while revoking permission %s"
                                      .formatted(permissionId));
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

            assertThat(logCaptor.getErrorLogs())
                    .contains("Error while sending connection status messages while revoking permission %s"
                                      .formatted(permissionId));
        }
    }

    @Nested
    @DisplayName("Tests whether the @PostConstruct annotated method of PermissionService updates the permissions correctly")
    class PermissionServiceTestUpdatePermissionsOnStartup {
        @BeforeEach
        void setUp() {
            clock = mock(Clock.class);
            service = new PermissionService(repository, clock, streamerManager, scheduler, permissionFutures);
        }

        /**
         * Tests that permissions are queried from the DB on startup and if their expiration time has passed, their
         * status is set accordingly or streaming is started again otherwise.
         * <p>
         * {@link PermissionServiceIntegrationTest} tests the same functionality but with a database and ensures that
         * the method is correctly called by Spring on startup.
         * </p>
         */
        @Test
        void givenVariousPermissions_statusAsExpected() {
            var codes = Set.of("1.8.0", "2.8.0");

            var start = Instant.parse("2023-09-01T00:00:00.000Z");
            var expiration = Instant.parse("2023-12-24T00:00:00.000Z");
            String shouldStreamPermissionId = "25ee5365-5d71-4b01-b21f-9c61f76a5cc9";
            var acceptedShouldBeStreamingData = new Permission(shouldStreamPermissionId,
                                                               "Should be STREAMING_DATA after test completes",
                                                               "SomeDataNeedId",
                                                               start,
                                                               expiration,
                                                               start,
                                                               "SomeConnectionId",
                                                               codes);

            start = Instant.parse("2023-09-01T00:00:00.000Z");
            expiration = Instant.parse("2023-09-19T00:00:00.000Z");
            String timeLimit1 = "9609a9b3-0718-4082-935d-6a98c0f8c5a2";
            var waitingForStartShouldBeTimeLimit = new Permission(timeLimit1,
                                                                  "Was WAITING_FOR_START and should be FULFILLED after test completes",
                                                                  "SomeDataNeedId",
                                                                  start,
                                                                  expiration,
                                                                  start,
                                                                  "SomeConnectionId",
                                                                  codes);
            waitingForStartShouldBeTimeLimit.updateStatus(PermissionStatus.WAITING_FOR_START);

            start = Instant.parse("2023-09-11T00:00:00.000Z");
            expiration = Instant.parse("2023-09-30T00:00:00.000Z");
            String timeLimit2 = "0b3b6f6d-d878-49dd-9dfd-62156b5cdc37";
            var streamingDataShouldBeTimeLimit = new Permission(timeLimit2,
                                                                "Was STREAMING_DATA and should be FULFILLED after test completes",
                                                                "SomeDataNeedId",
                                                                start,
                                                                expiration,
                                                                start,
                                                                "SomeConnectionId",
                                                                codes);
            streamingDataShouldBeTimeLimit.updateStatus(PermissionStatus.STREAMING_DATA);

            start = Instant.parse("2023-09-11T00:00:00.000Z");
            expiration = Instant.parse("2023-10-31T00:00:00.000Z");
            String streamingDataId = "f53aa9e2-1969-4d86-a5e0-d76b2fd72863";
            var streamingDataShouldBeStreamingData = new Permission(streamingDataId,
                                                                    "Was STREAMING_DATA and should be STREAMING_DATA after test completes",
                                                                    "SomeDataNeedId",
                                                                    start,
                                                                    expiration,
                                                                    start,
                                                                    "SomeConnectionId",
                                                                    codes);
            streamingDataShouldBeStreamingData.updateStatus(PermissionStatus.STREAMING_DATA);

            doReturn(Instant.parse("2023-10-01T12:00:00.00Z")).when(clock).instant();
            when(repository.findAllActivePermissions()).thenReturn(List.of(acceptedShouldBeStreamingData,
                                                                           waitingForStartShouldBeTimeLimit,
                                                                           streamingDataShouldBeTimeLimit,
                                                                           streamingDataShouldBeStreamingData));
            Map<String, Permission> savedByMethod = new HashMap<>();
            when(repository.save(any(Permission.class))).thenAnswer(i -> {
                var per = (Permission) i.getArgument(0);
                savedByMethod.put(per.permissionId(), per);
                return per;
            });
            doReturn(mockScheduledFuture).when(scheduler).schedule(any(), any(Instant.class));

            service.onApplicationEvent(mock(ContextRefreshedEvent.class));

            var permission = savedByMethod.get(shouldStreamPermissionId);
            assertEquals(PermissionStatus.STREAMING_DATA, permission.status());
            verify(streamerManager).createNewStreamerForPermission(argThat(arg ->
                                                                                   arg.permissionId()
                                                                                      .equals(shouldStreamPermissionId)));

            permission = savedByMethod.get(timeLimit1);
            assertEquals(PermissionStatus.FULFILLED, permission.status());

            permission = savedByMethod.get(timeLimit2);
            assertEquals(PermissionStatus.FULFILLED, permission.status());

            permission = savedByMethod.get(streamingDataId);
            assertEquals(PermissionStatus.STREAMING_DATA, permission.status());
            verify(streamerManager).createNewStreamerForPermission(argThat(arg ->
                                                                                   arg.permissionId()
                                                                                      .equals(streamingDataId)));

            verify(scheduler, times(2)).schedule(any(), any(Instant.class));
            verify(permissionFutures).put(eq(shouldStreamPermissionId), any());
            verify(permissionFutures).put(eq(streamingDataId), any());
        }
    }
}
