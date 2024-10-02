package energy.eddie.aiida.services;

import energy.eddie.aiida.dtos.PermissionDetailsDto;
import energy.eddie.aiida.errors.*;
import energy.eddie.aiida.models.permission.AiidaLocalDataNeed;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.streamers.StreamerManager;
import energy.eddie.api.agnostic.aiida.MqttDto;
import energy.eddie.api.agnostic.aiida.QrCodeDto;
import energy.eddie.api.agnostic.process.model.PermissionStateTransitionException;
import energy.eddie.dataneeds.needs.aiida.GenericAiidaDataNeed;
import org.junit.jupiter.api.*;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.client.HttpClientErrorException;
import reactor.core.publisher.Mono;
import reactor.test.publisher.TestPublisher;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Stream;

import static energy.eddie.aiida.config.AiidaConfiguration.AIIDA_ZONE_ID;
import static energy.eddie.aiida.models.permission.PermissionStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {
    @Mock
    private PermissionRepository mockRepository;
    @Mock
    private StreamerManager streamerManager;
    @Mock
    private PermissionScheduler mockPermissionScheduler;
    @Mock
    private HandshakeService mockHandshakeService;
    @Mock
    private GenericAiidaDataNeed mockDataNeed;
    @Mock
    private AiidaLocalDataNeed mockAiidaDataNeed;
    @Mock
    private AuthService mockAuthService;
    @Spy
    private Permission mockPermission;
    @Mock
    private MqttDto mockMqttDto;
    @Captor
    private ArgumentCaptor<Permission> permissionCaptor;
    private PermissionService service;
    private final String permissionId = "72831e2c-a01c-41b8-9db6-3f51670df7a5";
    private final String handshakeUrl = "https://example.org";
    private final String accessToken = "fooBar";
    private final String serviceName = "Hello Service";
    private final String connectionId = "NewAiidaRandomConnectionId";
    private final LocalDate start = LocalDate.of(2020, 1, 1);
    private final LocalDate end = LocalDate.of(2020, 5, 31);
    private final TestPublisher<String> testPublisher = TestPublisher.create();
    private final Instant fixedInstant = Instant.parse("2023-09-11T22:00:00.00Z");
    private final Clock clock = Clock.fixed(fixedInstant, AIIDA_ZONE_ID);
    private final UUID userId = UUID.randomUUID();

    private final QrCodeDto qrCodeDto = new QrCodeDto(permissionId, serviceName, handshakeUrl, accessToken);

    @BeforeEach
    void setUp() {
        doReturn(testPublisher.flux()).when(streamerManager).terminationRequestsFlux();

        service = new PermissionService(mockRepository,
                                        clock,
                                        streamerManager,
                                        mockHandshakeService,
                                        mockPermissionScheduler,
                                        mockAuthService);
    }

    @Test
    void givenTerminationRequest_removesFromPermissionScheduler_andStopsStreamer() {
        // Given
        when(mockRepository.findById(permissionId)).thenReturn(Optional.of(mockPermission));
        when(mockRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        when(mockPermission.permissionId()).thenReturn(permissionId);
        when(mockPermission.connectionId()).thenReturn(connectionId);
        when(mockPermission.dataNeed()).thenReturn(mockAiidaDataNeed);
        when(mockAiidaDataNeed.dataNeedId()).thenReturn("dataNeedId");

        // When
        testPublisher.next(permissionId);

        // Then
        testPublisher.assertSubscribers(1);
        verify(mockPermissionScheduler).removePermission(permissionId);
        verify(streamerManager).stopStreamer(argThat(msg -> msg.status() == TERMINATED));
        verify(mockPermission).setStatus(TERMINATED);
        verify(mockPermission).setRevokeTime(any());
        verify(mockRepository).save(any());
    }

    @Test
    void givenNotExistingPermission_findById_throws() {
        var notExistingId = "NotExistingId";

        when(mockRepository.findById(notExistingId)).thenReturn(Optional.empty());

        assertThrows(PermissionNotFoundException.class, () -> service.findById(notExistingId));
    }

    @Test
    void givenExistingPermissionId_setupPermission_throws() throws InvalidUserException {
        // Given
        when(mockRepository.existsById(permissionId)).thenReturn(true);
        when(mockAuthService.getCurrentUserId()).thenReturn(userId);

        // When, Then
        assertThrows(PermissionAlreadyExistsException.class, () -> service.setupNewPermission(qrCodeDto));
    }

    @Test
    void givenExceptionFromHandshakeService_setupNewPermission_throws() throws InvalidUserException {
        // Given
        var exception = HttpClientErrorException.create(HttpStatus.GONE,
                                                        "Gone",
                                                        HttpHeaders.EMPTY,
                                                        "".getBytes(StandardCharsets.UTF_8),
                                                        StandardCharsets.UTF_8);
        when(mockHandshakeService.fetchDetailsForPermission(any())).thenReturn(Mono.error(exception));
        when(mockRepository.save(any())).thenReturn(mockPermission);
        when(mockPermission.permissionId()).thenReturn(permissionId);
        when(mockAuthService.getCurrentUserId()).thenReturn(userId);

        // When, Then
        assertThrows(DetailFetchingFailedException.class, () -> service.setupNewPermission(qrCodeDto));
    }

    @Test
    void givenValidQrCodeDto_setupNewPermission_savesToDb_andCallsHandshakeService() throws PermissionUnfulfillableException, PermissionAlreadyExistsException, DetailFetchingFailedException, InvalidUserException {
        // Given
        var expectedStart = ZonedDateTime.of(start, LocalTime.MIN, AIIDA_ZONE_ID).toInstant();
        var expectedEnd = ZonedDateTime.of(end, LocalTime.MAX.withNano(0), AIIDA_ZONE_ID).toInstant();
        var permissionDetails = new PermissionDetailsDto(permissionId, connectionId, start, end, mockDataNeed);
        when(mockRepository.existsById(permissionId)).thenReturn(false);
        when(mockRepository.save(any(Permission.class))).then(i -> i.getArgument(0));
        when(mockHandshakeService.fetchDetailsForPermission(any())).thenReturn(Mono.just(permissionDetails));
        when(mockDataNeed.transmissionInterval()).thenReturn(23);
        when(mockDataNeed.id()).thenReturn("myId");
        when(mockDataNeed.type()).thenReturn(GenericAiidaDataNeed.DISCRIMINATOR_VALUE);
        when(mockDataNeed.name()).thenReturn("My Name");
        when(mockDataNeed.purpose()).thenReturn("Some purpose");
        when(mockDataNeed.policyLink()).thenReturn("https://example.org");
        when(mockDataNeed.dataTags()).thenReturn(Set.of("1.8.0", "2.7.0"));
        when(mockAuthService.getCurrentUserId()).thenReturn(userId);

        // When
        service.setupNewPermission(qrCodeDto);

        // Then
        verify(mockHandshakeService).fetchDetailsForPermission(argThat(arg -> arg.permissionId().equals(permissionId)));
        verify(mockRepository, times(2)).save(permissionCaptor.capture());

        // As we verify after we modified the permission object twice, we can only assert the last modifications because the two captured objects are the same, see https://groups.google.com/g/mockito/c/KBRocVedYT0/m/UL-QJfNtvhoJ
        Permission permissionWithDetails = permissionCaptor.getAllValues().getLast();
        // verify all fields have been set
        assertEquals(permissionId, permissionWithDetails.permissionId());
        assertEquals(FETCHED_DETAILS, permissionWithDetails.status());
        assertEquals(connectionId, permissionWithDetails.connectionId());
        assertEquals(expectedStart, permissionWithDetails.startTime());
        assertEquals(expectedEnd, permissionWithDetails.expirationTime());

        // also check for data need fields
        AiidaLocalDataNeed dataNeed = permissionWithDetails.dataNeed();
        assertNotNull(dataNeed);
        assertEquals(permissionId, dataNeed.permissionId());
        assertEquals("myId", dataNeed.dataNeedId());
        assertEquals(23, dataNeed.transmissionInterval());
        assertEquals(GenericAiidaDataNeed.DISCRIMINATOR_VALUE, dataNeed.type());
        assertEquals("My Name", dataNeed.name());
        assertEquals("Some purpose", dataNeed.purpose());
        assertEquals("https://example.org", dataNeed.policyLink());
        assertThat(dataNeed.dataTags()).hasSameElementsAs(Set.of("1.8.0", "2.7.0"));
    }

    @Disabled("// TODO GH-1040")  // TODO GH-1040
    @Test
    void givenUnfulfillableQrCodeDto_setupNewPermission_updatesStatus() {

        // When
        assertThrows(PermissionUnfulfillableException.class, () -> service.setupNewPermission(qrCodeDto));

        // Then
        verify(mockHandshakeService).fetchDetailsForPermission(argThat(arg -> arg.permissionId().equals(permissionId)));
        verify(mockHandshakeService).sendUnfulfillableOrRejected(any(), eq(UNFULFILLABLE));
        verify(mockRepository, times(2)).save(permissionCaptor.capture());
        assertEquals(permissionId, permissionCaptor.getAllValues().getFirst().permissionId());
        assertEquals(CREATED, permissionCaptor.getAllValues().getFirst().status());
        assertEquals(permissionId, permissionCaptor.getAllValues().get(1).permissionId());
        assertEquals(UNFULFILLABLE, permissionCaptor.getAllValues().get(1).status());
    }

    @Test
    void givenPermissionInInvalidState_rejectPermission_throws() {
        // Given
        when(mockRepository.findById(permissionId)).thenReturn(Optional.of(mockPermission));
        when(mockPermission.status()).thenReturn(STREAMING_DATA);

        // When, Then
        assertThrows(PermissionStateTransitionException.class, () -> service.rejectPermission(permissionId));
    }

    @Test
    void givenValidPermission_rejectPermission_updatesState_andRecordsTimestamp_andCallsHandshakeService() throws PermissionStateTransitionException, PermissionNotFoundException, UnauthorizedException, InvalidUserException {
        // Given
        when(mockRepository.findById(permissionId)).thenReturn(Optional.of(mockPermission));
        when(mockPermission.status()).thenReturn(FETCHED_DETAILS);

        // When
        service.rejectPermission(permissionId);

        // Then
        verify(mockHandshakeService).sendUnfulfillableOrRejected(any(), eq(REJECTED));
        verify(mockPermission).setStatus(REJECTED);
        verify(mockPermission).setRevokeTime(fixedInstant);
        verify(mockRepository).save(any());
    }

    @Test
    void givenPermissionInInvalidState_acceptPermission_throws() {
        // Given
        when(mockRepository.findById(permissionId)).thenReturn(Optional.of(mockPermission));
        when(mockPermission.status()).thenReturn(STREAMING_DATA);

        // When, Then
        assertThrows(PermissionStateTransitionException.class, () -> service.acceptPermission(permissionId));
    }

    @Test
    void givenExceptionFromHandshakeService_acceptPermission_throws() {
        // Given
        var exception = HttpClientErrorException.create(HttpStatus.GONE,
                                                        "Gone",
                                                        HttpHeaders.EMPTY,
                                                        "".getBytes(StandardCharsets.UTF_8),
                                                        StandardCharsets.UTF_8);
        when(mockHandshakeService.fetchMqttDetails(any())).thenReturn(Mono.error(exception));
        when(mockRepository.findById(any())).thenReturn(Optional.of(mockPermission));
        when(mockPermission.status()).thenReturn(FETCHED_DETAILS);

        // When, Then
        assertThrows(DetailFetchingFailedException.class, () -> service.acceptPermission(permissionId));
    }

    @Test
    void givenValidPermission_acceptPermission_updatesState_callsHandshakeService_andPermissionScheduler() throws PermissionStateTransitionException, PermissionNotFoundException, DetailFetchingFailedException, UnauthorizedException, InvalidUserException {
        // Given
        when(mockRepository.save(any(Permission.class))).then(i -> i.getArgument(0));
        when(mockRepository.findById(permissionId)).thenReturn(Optional.of(mockPermission));
        when(mockPermission.permissionId()).thenReturn(permissionId);
        when(mockPermission.status()).thenReturn(FETCHED_DETAILS);
        when(mockHandshakeService.fetchMqttDetails(any())).thenReturn(Mono.just(mockMqttDto));
        when(mockMqttDto.dataTopic()).thenReturn("dataTopic");

        // When
        service.acceptPermission(permissionId);

        // Then
        verify(mockHandshakeService).fetchMqttDetails(any());
        verify(mockPermission).setStatus(ACCEPTED);
        verify(mockPermission).setStatus(FETCHED_MQTT_CREDENTIALS);
        verify(mockPermission).setGrantTime(fixedInstant);
        verify(mockPermission).setMqttStreamingConfig(assertArg(conf -> assertAll(
                () -> assertEquals("dataTopic", conf.dataTopic()),
                () -> assertEquals(permissionId, conf.permissionId())
        )));
        verify(mockRepository, times(2)).save(any());
        verify(mockPermissionScheduler).scheduleOrStart(any());
    }

    @Test
    void givenInvalidUser_getPermissions_throws() throws InvalidUserException {
        // Given
        when(mockAuthService.getCurrentUserId()).thenThrow(InvalidUserException.class);

        // When, Then
        assertThrows(InvalidUserException.class, () -> service.getAllPermissionsSortedByGrantTime());
    }

    @Test
    void givenValidUser_getPermissions() throws InvalidUserException {
        // Given
        var uuid = UUID.fromString("dc9ff3d3-1f1f-445d-a4ee-85c1faffb715");
        when(mockAuthService.getCurrentUserId()).thenReturn(uuid);
        when(mockRepository.findByUserIdOrderByGrantTimeDesc(uuid)).thenReturn(List.of(mockPermission));

        // When
        var result = service.getAllPermissionsSortedByGrantTime();

        // Then
        assertEquals(List.of(mockPermission), result);
    }

    @Test
    void givenPermissionInInvalidState_revokePermission_throws() {
        // Given
        when(mockRepository.findById(permissionId)).thenReturn(Optional.of(mockPermission));
        when(mockPermission.status()).thenReturn(REJECTED);

        // When, Then
        assertThrows(PermissionStateTransitionException.class, () -> service.revokePermission(permissionId));
    }

    @Test
    void givenValidPermission_revokePermission_updatesState_andRemovesScheduledRunnables() throws PermissionNotFoundException, PermissionStateTransitionException, UnauthorizedException, InvalidUserException {
        // Given
        when(mockRepository.save(any(Permission.class))).then(i -> i.getArgument(0));
        when(mockRepository.findById(permissionId)).thenReturn(Optional.of(mockPermission));
        when(mockPermission.connectionId()).thenReturn("connectionId");
        when(mockPermission.dataNeed()).thenReturn(mockAiidaDataNeed);
        when(mockAiidaDataNeed.dataNeedId()).thenReturn("dataNeedId");
        when(mockPermission.status()).thenReturn(STREAMING_DATA);

        // When
        service.revokePermission(permissionId);

        // Then
        verify(mockPermissionScheduler).removePermission(permissionId);
        verify(mockPermission).setStatus(REVOKED);
        verify(mockPermission).setRevokeTime(fixedInstant);
        verify(streamerManager).stopStreamer(argThat(msg -> msg.status() == REVOKED));
        verify(mockRepository).save(any());
    }

    @Nested
    @DisplayName("Tests whether the @PostConstruct annotated method of PermissionService updates the permissions correctly")
    class PermissionServiceTestUpdatePermissionsOnStartup {
        @Mock
        private Clock mockClock;
        @Mock
        private TaskScheduler mockScheduler;
        @Mock
        private ScheduledFuture<?> mockScheduledFuture;

        public static Stream<Arguments> onStartupVariousPermissions() {
            var per = new Permission(new QrCodeDto("25ee5365-5d71-4b01-b21f-9c61f76a5cc9",
                                                   "Test Service Name",
                                                   "https://example.org",
                                                   "fooBarToken"), UUID.randomUUID());
            return Stream.of(
                    Arguments.of(per, "2023-09-01T00:00:00.000Z", "2023-12-24T00:00:00.000Z", STREAMING_DATA),
                    Arguments.of(per, "2023-09-01T00:00:00.000Z", "2023-09-19T00:00:00.000Z", FULFILLED),
                    Arguments.of(per, "2023-09-11T00:00:00.000Z", "2023-09-30T00:00:00.000Z", FULFILLED),
                    Arguments.of(per, "2023-09-11T00:00:00.000Z", "2023-10-31T00:00:00.000Z", STREAMING_DATA),
                    Arguments.of(per, "2023-10-12T00:00:00.000Z", "2023-10-15T00:00:00.000Z", WAITING_FOR_START)
            );
        }

        @BeforeEach
        void setUp() {
            var permissionScheduler = new PermissionScheduler(mockClock,
                                                              mockScheduler,
                                                              mockRepository,
                                                              new ConcurrentHashMap<>(),
                                                              streamerManager);
            service = new PermissionService(mockRepository,
                                            mockClock,
                                            streamerManager,
                                            mockHandshakeService,
                                            permissionScheduler,
                                            mockAuthService);
        }

        /**
         * Tests that permissions are queried from the DB on startup and if their expiration time has passed, their
         * status is set accordingly or streaming is started again otherwise.
         * <p>
         * {@link PermissionServiceIntegrationTest} tests the same functionality but with a database and ensures that
         * the method is correctly called by Spring on startup.
         * </p>
         */
        @ParameterizedTest
        @MethodSource("onStartupVariousPermissions")
        void givenVariousPermissions_statusAsExpected(
                Permission permission,
                String start,
                String end,
                PermissionStatus expectedState
        ) {
            // Given
            doReturn(Instant.parse("2023-10-01T12:00:00.00Z")).when(mockClock).instant();
            permission.setStartTime(Instant.parse(start));
            permission.setExpirationTime(Instant.parse(end));
            when(mockRepository.findAllActivePermissions()).thenReturn(List.of(permission));

            // strict stubbing requires us to only stub the .schedule if a runnable will be scheduled
            if (expectedState == WAITING_FOR_START || expectedState == STREAMING_DATA)
                doReturn(mockScheduledFuture).when(mockScheduler).schedule(any(), any(Instant.class));

            // When
            service.onApplicationEvent(mock(ContextRefreshedEvent.class));

            // Then
            verify(mockRepository).save(assertArg(
                    per -> assertEquals(expectedState, per.status())
            ));
        }
    }
}
