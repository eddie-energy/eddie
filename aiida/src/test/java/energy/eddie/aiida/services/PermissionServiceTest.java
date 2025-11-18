package energy.eddie.aiida.services;

import energy.eddie.aiida.dtos.PermissionDetailsDto;
import energy.eddie.aiida.dtos.events.InboundPermissionAcceptEvent;
import energy.eddie.aiida.dtos.events.InboundPermissionRevokeEvent;
import energy.eddie.aiida.dtos.events.OutboundPermissionAcceptEvent;
import energy.eddie.aiida.errors.auth.InvalidUserException;
import energy.eddie.aiida.errors.auth.UnauthorizedException;
import energy.eddie.aiida.errors.permission.DetailFetchingFailedException;
import energy.eddie.aiida.errors.permission.PermissionAlreadyExistsException;
import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.aiida.errors.permission.PermissionUnfulfillableException;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import energy.eddie.aiida.models.permission.dataneed.AiidaLocalDataNeed;
import energy.eddie.aiida.models.permission.dataneed.InboundAiidaLocalDataNeed;
import energy.eddie.aiida.publisher.AiidaEventPublisher;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.streamers.StreamerManager;
import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.api.agnostic.aiida.QrCodeDto;
import energy.eddie.api.agnostic.aiida.mqtt.MqttDto;
import energy.eddie.api.agnostic.process.model.PermissionStateTransitionException;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.needs.aiida.AiidaDataNeed;
import energy.eddie.dataneeds.needs.aiida.AiidaSchema;
import energy.eddie.dataneeds.needs.aiida.OutboundAiidaDataNeed;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronExpression;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {
    private final UUID eddieId = UUID.fromString("a69f9bc2-e16c-4de4-8c3e-00d219dcd819");
    private final UUID permissionId = UUID.fromString("72831e2c-a01c-41b8-9db6-3f51670df7a5");
    private final UUID dataNeedId = UUID.fromString("82831e2c-a01c-41b8-9db6-3f51670df7a5");
    private final UUID dataSourceId = UUID.fromString("92831e2c-a01c-41b8-9db6-3f51670df7a5");
    private final String handshakeUrl = "https://example.org";
    private final String serviceName = "Hello Service";
    private final String connectionId = "NewAiidaRandomConnectionId";
    private final LocalDate start = LocalDate.now(ZoneId.systemDefault());
    private final LocalDate end = LocalDate.now(ZoneId.systemDefault()).plusDays(90);
    private final TestPublisher<UUID> testPublisher = TestPublisher.create();
    private final Instant fixedInstant = Instant.parse("2023-09-11T22:00:00.00Z");
    private final Clock clock = Clock.fixed(fixedInstant, AIIDA_ZONE_ID);
    private final UUID userId = UUID.randomUUID();
    private final QrCodeDto qrCodeDto = new QrCodeDto(eddieId, permissionId, serviceName, handshakeUrl);
    @Mock
    private PermissionRepository mockPermissionRepository;
    @Mock
    private AiidaEventPublisher mockAiidaEventPublisher;
    @Mock
    private StreamerManager streamerManager;
    @Mock
    private PermissionScheduler mockPermissionScheduler;
    @Mock
    private HandshakeService mockHandshakeService;
    @Mock
    private AiidaDataNeed mockDataNeed;
    @Mock
    private InboundAiidaLocalDataNeed mockInboundAiidaLocalDataNeed;
    @Mock
    private AuthService mockAuthService;
    @Mock
    private Permission mockPermission;
    @Mock
    private DataSource mockDataSource;
    @Mock
    private MqttDto mockMqttDto;
    @Mock
    private AiidaLocalDataNeedService mockAiidaLocalDataNeedService;
    @Captor
    private ArgumentCaptor<Permission> permissionCaptor;
    private PermissionService service;

    @BeforeEach
    void setUp() {
        doReturn(testPublisher.flux()).when(streamerManager).terminationRequestsFlux();

        service = new PermissionService(mockPermissionRepository,
                                        clock,
                                        streamerManager,
                                        mockHandshakeService,
                                        mockPermissionScheduler,
                                        mockAuthService,
                                        mockAiidaLocalDataNeedService,
                                        mockAiidaEventPublisher);
    }

    @Test
    void givenTerminationRequest_removesFromPermissionScheduler_andStopsStreamer() {
        // Given
        when(mockPermissionRepository.findById(permissionId)).thenReturn(Optional.of(mockPermission));
        when(mockPermissionRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        when(mockPermission.id()).thenReturn(permissionId);
        when(mockPermission.connectionId()).thenReturn(connectionId);
        when(mockPermission.dataNeed()).thenReturn(mockInboundAiidaLocalDataNeed);
        when(mockPermission.dataSource()).thenReturn(mockDataSource);

        // When
        testPublisher.next(permissionId);

        // Then
        testPublisher.assertSubscribers(1);
        verify(mockPermissionScheduler).removePermission(permissionId);
        verify(streamerManager).stopStreamer(argThat(msg -> msg.status() == PermissionProcessStatus.EXTERNALLY_TERMINATED));
        verify(mockPermission).setStatus(PermissionStatus.TERMINATED);
        verify(mockPermission).setRevokeTime(any());
        verify(mockPermissionRepository).save(any());
    }

    @Test
    void givenNotExistingPermission_findById_throws() {
        var notExistingId = UUID.fromString("12831e2c-a01c-41b8-9db6-3f51670df7a5");

        when(mockPermissionRepository.findById(notExistingId)).thenReturn(Optional.empty());

        assertThrows(PermissionNotFoundException.class, () -> service.findById(notExistingId));
    }

    @Test
    void givenExistingPermissionId_setupPermission_throws() throws InvalidUserException {
        // Given
        when(mockPermissionRepository.existsById(permissionId)).thenReturn(true);
        when(mockAuthService.getCurrentUserId()).thenReturn(userId);

        // When, Then
        assertThrows(PermissionAlreadyExistsException.class, () -> service.setupNewPermission(qrCodeDto));
    }

    @Test
    void givenExceptionFromHandshakeService_setupNewPermission_throwsDetailFetchingFailedException() throws InvalidUserException {
        // Given
        var exception = HttpClientErrorException.create(HttpStatus.GONE,
                                                        "Gone",
                                                        HttpHeaders.EMPTY,
                                                        "".getBytes(StandardCharsets.UTF_8),
                                                        StandardCharsets.UTF_8);
        when(mockHandshakeService.fetchDetailsForPermission(any())).thenReturn(Mono.error(exception));
        when(mockPermissionRepository.save(any())).thenReturn(mockPermission);
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
        when(mockPermissionRepository.existsById(permissionId)).thenReturn(false);
        when(mockPermissionRepository.save(any(Permission.class))).then(i -> i.getArgument(0));
        when(mockHandshakeService.fetchDetailsForPermission(any())).thenReturn(Mono.just(permissionDetails));
        when(mockDataNeed.transmissionSchedule()).thenReturn(CronExpression.parse("*/23 * * * * *"));
        when(mockDataNeed.dataNeedId()).thenReturn(dataNeedId);
        when(mockDataNeed.type()).thenReturn(OutboundAiidaDataNeed.DISCRIMINATOR_VALUE);
        when(mockDataNeed.name()).thenReturn("My Name");
        when(mockDataNeed.purpose()).thenReturn("Some purpose");
        when(mockDataNeed.policyLink()).thenReturn("https://example.org");
        when(mockDataNeed.dataTags()).thenReturn(Set.of(ObisCode.POSITIVE_ACTIVE_ENERGY,
                                                        ObisCode.NEGATIVE_ACTIVE_ENERGY));
        when(mockAuthService.getCurrentUserId()).thenReturn(userId);
        when(mockDataNeed.schemas()).thenReturn(Set.of(AiidaSchema.SMART_METER_P1_RAW));

        // When
        service.setupNewPermission(qrCodeDto);

        // Then
        verify(mockHandshakeService).fetchDetailsForPermission(argThat(arg -> arg.id().equals(permissionId)));
        verify(mockPermissionRepository, times(2)).save(permissionCaptor.capture());

        // As we verify after we modified the permission object twice, we can only assert the last modifications because the two captured objects are the same, see https://groups.google.com/g/mockito/c/KBRocVedYT0/m/UL-QJfNtvhoJ
        Permission permissionWithDetails = permissionCaptor.getAllValues().getLast();
        // verify all fields have been set
        assertEquals(eddieId, permissionWithDetails.eddieId());
        assertEquals(permissionId, permissionWithDetails.id());
        assertEquals(PermissionStatus.FETCHED_DETAILS, permissionWithDetails.status());
        assertEquals(connectionId, permissionWithDetails.connectionId());
        assertEquals(expectedStart, permissionWithDetails.startTime());
        assertEquals(expectedEnd, permissionWithDetails.expirationTime());

        // also check for data need fields
        AiidaLocalDataNeed dataNeed = permissionWithDetails.dataNeed();
        assertNotNull(dataNeed);
        assertEquals(dataNeedId, dataNeed.dataNeedId());
        assertEquals("*/23 * * * * *", dataNeed.transmissionSchedule().toString());
        assertEquals(OutboundAiidaDataNeed.DISCRIMINATOR_VALUE, dataNeed.type());
        assertThat(dataNeed.dataTags()).hasSameElementsAs(Set.of(ObisCode.POSITIVE_ACTIVE_ENERGY,
                                                                 ObisCode.NEGATIVE_ACTIVE_ENERGY));
        assertThat(dataNeed.schemas()).hasSameElementsAs(Set.of(AiidaSchema.SMART_METER_P1_RAW));
    }

    @Test
    void setupNewPermission_throwsPermissionUnfulfillableException() throws InvalidUserException {
        // Given
        var expectedStart = ZonedDateTime.of(start, LocalTime.MIN, AIIDA_ZONE_ID).toInstant();
        var expectedEnd = ZonedDateTime.of(end, LocalTime.MAX.withNano(0), AIIDA_ZONE_ID).toInstant();
        var permissionDetails = new PermissionDetailsDto(permissionId, connectionId, start, end, mockDataNeed);
        when(mockPermissionRepository.existsById(permissionId)).thenReturn(false);
        when(mockPermissionRepository.save(any(Permission.class))).then(i -> i.getArgument(0));
        when(mockHandshakeService.fetchDetailsForPermission(any())).thenReturn(Mono.just(permissionDetails));
        when(mockDataNeed.transmissionSchedule()).thenReturn(CronExpression.parse("*/23 * * * * *"));
        when(mockDataNeed.dataNeedId()).thenReturn(dataNeedId);
        when(mockDataNeed.type()).thenReturn("illegalValue");
        when(mockDataNeed.name()).thenReturn("My Name");
        when(mockDataNeed.purpose()).thenReturn("Some purpose");
        when(mockDataNeed.policyLink()).thenReturn("https://example.org");
        when(mockDataNeed.dataTags()).thenReturn(Set.of(ObisCode.POSITIVE_ACTIVE_ENERGY,
                                                        ObisCode.NEGATIVE_ACTIVE_ENERGY));
        when(mockAuthService.getCurrentUserId()).thenReturn(userId);
        when(mockDataNeed.schemas()).thenReturn(Set.of(AiidaSchema.SMART_METER_P1_RAW));

        // When Then
        assertThrows(PermissionUnfulfillableException.class, () -> service.setupNewPermission(qrCodeDto));
        verify(mockHandshakeService).fetchDetailsForPermission(argThat(arg -> arg.id().equals(permissionId)));
        verify(mockPermissionRepository, times(2)).save(permissionCaptor.capture());

        // As we verify after we modified the permission object twice, we can only assert the last modifications because the two captured objects are the same, see https://groups.google.com/g/mockito/c/KBRocVedYT0/m/UL-QJfNtvhoJ
        Permission permissionWithDetails = permissionCaptor.getAllValues().getLast();
        // verify all fields have been set
        assertEquals(eddieId, permissionWithDetails.eddieId());
        assertEquals(permissionId, permissionWithDetails.id());
        assertEquals(PermissionStatus.UNFULFILLABLE, permissionWithDetails.status());
        assertEquals(connectionId, permissionWithDetails.connectionId());
        assertEquals(expectedStart, permissionWithDetails.startTime());
        assertEquals(expectedEnd, permissionWithDetails.expirationTime());

        // also check for data need fields
        AiidaLocalDataNeed dataNeed = permissionWithDetails.dataNeed();
        assertNotNull(dataNeed);
        assertEquals(dataNeedId, dataNeed.dataNeedId());
        assertEquals("*/23 * * * * *", dataNeed.transmissionSchedule().toString());
        assertEquals("illegalValue", dataNeed.type());
        assertThat(dataNeed.dataTags()).hasSameElementsAs(Set.of(ObisCode.POSITIVE_ACTIVE_ENERGY,
                                                                 ObisCode.NEGATIVE_ACTIVE_ENERGY));
        assertThat(dataNeed.schemas()).hasSameElementsAs(Set.of(AiidaSchema.SMART_METER_P1_RAW));
    }

    @Test
    void givenStartDateInThePast_setsStatusToUnfulfillable_andCallsHandshakeService() {
        // Given
        var permissionDetails = new PermissionDetailsDto(permissionId,
                                                         connectionId,
                                                         start.minusDays(1),
                                                         end,
                                                         mockDataNeed);
        when(mockPermissionRepository.existsById(permissionId)).thenReturn(false);
        when(mockPermissionRepository.save(any(Permission.class))).then(i -> i.getArgument(0));
        when(mockHandshakeService.fetchDetailsForPermission(any())).thenReturn(Mono.just(permissionDetails));

        // When
        assertThrows(PermissionUnfulfillableException.class, () -> service.setupNewPermission(qrCodeDto));

        // Then
        verify(mockHandshakeService).fetchDetailsForPermission(argThat(arg -> arg.id().equals(permissionId)));
        verify(mockHandshakeService).sendUnfulfillableOrRejected(any(), eq(PermissionStatus.UNFULFILLABLE));
        verify(mockPermissionRepository, times(2)).save(permissionCaptor.capture());
        assertEquals(permissionId, permissionCaptor.getAllValues().getFirst().id());
        assertEquals(PermissionStatus.UNFULFILLABLE, permissionCaptor.getAllValues().getFirst().status());
        assertEquals(permissionId, permissionCaptor.getAllValues().get(1).id());
        assertEquals(PermissionStatus.UNFULFILLABLE, permissionCaptor.getAllValues().get(1).status());
    }

    @Disabled("// TODO GH-1040")  // TODO GH-1040
    @Test
    void givenUnfulfillableQrCodeDto_setupNewPermission_updatesStatus() {

        // When
        assertThrows(PermissionUnfulfillableException.class, () -> service.setupNewPermission(qrCodeDto));

        // Then
        verify(mockHandshakeService).fetchDetailsForPermission(argThat(arg -> arg.id().equals(permissionId)));
        verify(mockHandshakeService).sendUnfulfillableOrRejected(any(), eq(PermissionStatus.UNFULFILLABLE));
        verify(mockPermissionRepository, times(2)).save(permissionCaptor.capture());
        assertEquals(permissionId, permissionCaptor.getAllValues().getFirst().id());
        assertEquals(PermissionStatus.CREATED, permissionCaptor.getAllValues().getFirst().status());
        assertEquals(permissionId, permissionCaptor.getAllValues().get(1).id());
        assertEquals(PermissionStatus.UNFULFILLABLE, permissionCaptor.getAllValues().get(1).status());
    }

    @Test
    void givenPermissionInInvalidState_rejectPermission_throws() {
        // Given
        when(mockPermissionRepository.findById(permissionId)).thenReturn(Optional.of(mockPermission));
        when(mockPermission.id()).thenReturn(permissionId);
        when(mockPermission.status()).thenReturn(PermissionStatus.STREAMING_DATA);

        // When, Then
        assertThrows(PermissionStateTransitionException.class, () -> service.rejectPermission(permissionId));
    }

    @Test
    void givenValidPermission_rejectPermission_updatesState_andRecordsTimestamp_andCallsHandshakeService() throws PermissionStateTransitionException, PermissionNotFoundException, UnauthorizedException, InvalidUserException {
        // Given
        when(mockPermissionRepository.findById(permissionId)).thenReturn(Optional.of(mockPermission));
        when(mockPermission.status()).thenReturn(PermissionStatus.FETCHED_DETAILS);

        // When
        service.rejectPermission(permissionId);

        // Then
        verify(mockHandshakeService).sendUnfulfillableOrRejected(any(), eq(PermissionStatus.REJECTED));
        verify(mockPermission).setStatus(PermissionStatus.REJECTED);
        verify(mockPermission).setRevokeTime(fixedInstant);
    }

    @Test
    void givenPermissionInInvalidState_acceptPermission_throws() {
        // Given
        when(mockPermissionRepository.findById(permissionId)).thenReturn(Optional.of(mockPermission));
        when(mockPermission.id()).thenReturn(permissionId);
        when(mockPermission.status()).thenReturn(PermissionStatus.STREAMING_DATA);

        // When, Then
        assertThrows(PermissionStateTransitionException.class,
                     () -> service.acceptPermission(permissionId, dataSourceId));
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
        when(mockPermissionRepository.findById(any())).thenReturn(Optional.of(mockPermission));
        when(mockPermissionRepository.save(any(Permission.class))).thenReturn(mockPermission);
        when(mockPermission.id()).thenReturn(permissionId);
        when(mockPermission.status()).thenReturn(PermissionStatus.FETCHED_DETAILS);

        // When, Then
        assertThrows(DetailFetchingFailedException.class, () -> service.acceptPermission(permissionId, dataSourceId));
    }

    @Test
    void givenValidPermission_acceptPermission_updatesState_callsHandshakeService_andPermissionScheduler() throws PermissionStateTransitionException, PermissionNotFoundException, DetailFetchingFailedException, UnauthorizedException, InvalidUserException {
        // Given
        when(mockPermissionRepository.save(any(Permission.class))).then(i -> i.getArgument(0));
        when(mockPermissionRepository.findById(permissionId)).thenReturn(Optional.of(mockPermission));
        when(mockPermission.status()).thenReturn(PermissionStatus.FETCHED_DETAILS);
        when(mockHandshakeService.fetchMqttDetails(any())).thenReturn(Mono.just(mockMqttDto));
        when(mockMqttDto.dataTopic()).thenReturn("dataTopic");
        when(mockMqttDto.username()).thenReturn(permissionId.toString());

        // When
        service.acceptPermission(permissionId, dataSourceId);

        // Then
        verify(mockHandshakeService).fetchMqttDetails(any());
        verify(mockPermission).setStatus(PermissionStatus.ACCEPTED);
        verify(mockPermission).setStatus(PermissionStatus.FETCHED_MQTT_CREDENTIALS);
        verify(mockPermission).setGrantTime(fixedInstant);
        verify(mockPermission).setMqttStreamingConfig(assertArg(conf -> assertAll(() -> assertEquals("dataTopic",
                                                                                                     conf.dataTopic()),
                                                                                  () -> assertEquals(permissionId,
                                                                                                     conf.permissionId()))));
        verify(mockPermissionRepository, times(1)).save(any());
        verify(mockPermissionScheduler).scheduleOrStart(any());
        verify(mockAiidaEventPublisher, times(1)).publishEvent(any(OutboundPermissionAcceptEvent.class));
    }

    @Test
    void givenValidInboundPermission_createsAndStartsDataSource() throws PermissionStateTransitionException, PermissionNotFoundException, DetailFetchingFailedException, UnauthorizedException, InvalidUserException {
        // Given
        when(mockPermissionRepository.save(any(Permission.class))).then(i -> i.getArgument(0));
        when(mockPermissionRepository.findById(permissionId)).thenReturn(Optional.of(mockPermission));
        when(mockPermission.status()).thenReturn(PermissionStatus.FETCHED_DETAILS);
        when(mockHandshakeService.fetchMqttDetails(any())).thenReturn(Mono.just(mockMqttDto));
        when(mockMqttDto.dataTopic()).thenReturn("dataTopic");
        when(mockMqttDto.username()).thenReturn(permissionId.toString());
        when(mockPermission.dataNeed()).thenReturn(mockInboundAiidaLocalDataNeed);

        // When
        service.acceptPermission(permissionId, null);

        // Then
        verify(mockAiidaEventPublisher, times(1)).publishEvent(any(InboundPermissionAcceptEvent.class));
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
        when(mockPermissionRepository.findByUserIdOrderByGrantTimeDesc(uuid)).thenReturn(List.of(mockPermission));

        // When
        var result = service.getAllPermissionsSortedByGrantTime();

        // Then
        assertEquals(List.of(mockPermission), result);
    }

    @Test
    void givenPermissionInInvalidState_revokePermission_throws() {
        // Given
        when(mockPermissionRepository.findById(permissionId)).thenReturn(Optional.of(mockPermission));
        when(mockPermission.id()).thenReturn(permissionId);
        when(mockPermission.status()).thenReturn(PermissionStatus.REJECTED);

        // When, Then
        assertThrows(PermissionStateTransitionException.class, () -> service.revokePermission(permissionId));
    }

    @Test
    void givenValidPermission_revokePermission_updatesState_andRemovesScheduledRunnables() throws PermissionNotFoundException, PermissionStateTransitionException, UnauthorizedException, InvalidUserException {
        // Given
        when(mockPermissionRepository.findById(permissionId)).thenReturn(Optional.of(mockPermission));
        when(mockPermission.connectionId()).thenReturn("connectionId");
        when(mockPermission.dataNeed()).thenReturn(mockInboundAiidaLocalDataNeed);
        when(mockPermission.status()).thenReturn(PermissionStatus.STREAMING_DATA);
        when(mockPermission.dataSource()).thenReturn(mockDataSource);
        when(mockPermission.id()).thenReturn(permissionId);

        // When
        service.revokePermission(permissionId);

        // Then
        verify(mockPermissionScheduler).removePermission(permissionId);
        verify(mockPermission).setStatus(PermissionStatus.REVOKED);
        verify(mockPermission).setRevokeTime(fixedInstant);
        verify(streamerManager).stopStreamer(argThat(msg -> msg.status() == PermissionProcessStatus.REVOKED));
    }

    @Test
    void givenValidInboundPermission_revokePermission_updatesState_andRemovesScheduledRunnables() throws PermissionNotFoundException, PermissionStateTransitionException, UnauthorizedException, InvalidUserException {
        // Given
        when(mockPermissionRepository.findById(permissionId)).thenReturn(Optional.of(mockPermission));
        when(mockPermission.connectionId()).thenReturn("connectionId");
        when(mockPermission.dataNeed()).thenReturn(mockInboundAiidaLocalDataNeed);
        when(mockPermission.status()).thenReturn(PermissionStatus.STREAMING_DATA);
        when(mockPermission.dataSource()).thenReturn(mockDataSource);
        when(mockPermission.id()).thenReturn(permissionId);
        when(mockPermission.dataSource()).thenReturn(mockDataSource);
        when(mockDataSource.type()).thenReturn(DataSourceType.INBOUND);

        // When
        service.revokePermission(permissionId);

        // Then
        verify(mockPermissionScheduler).removePermission(permissionId);
        verify(mockPermission).setStatus(PermissionStatus.REVOKED);
        verify(mockPermission).setRevokeTime(fixedInstant);
        verify(streamerManager).stopStreamer(argThat(msg -> msg.status() == PermissionProcessStatus.REVOKED));
        verify(mockAiidaEventPublisher, times(1)).publishEvent(any(InboundPermissionRevokeEvent.class));
    }

    @Test
    void testAddDatasourceToPermission() {
        // Given
        var datasource = mock(DataSource.class);

        // When
        when(mockPermissionRepository.findById(permissionId)).thenReturn(Optional.of(mockPermission));
        service.addDataSourceToPermission(datasource, permissionId);

        // Then
        verify(mockPermission).setDataSource(datasource);
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
            var per = new Permission(new QrCodeDto(UUID.fromString("15ee5365-5d71-4b01-b21f-9c61f76a5cc9"),
                                                   UUID.fromString("25ee5365-5d71-4b01-b21f-9c61f76a5cc9"),
                                                   "Test Service Name",
                                                   "https://example.org"), UUID.randomUUID());
            return Stream.of(Arguments.of(per, "2023-09-01T00:00:00.000Z", "2023-12-24T00:00:00.000Z", PermissionStatus.STREAMING_DATA),
                             Arguments.of(per, "2023-09-01T00:00:00.000Z", "2023-09-19T00:00:00.000Z", PermissionStatus.FULFILLED),
                             Arguments.of(per, "2023-09-11T00:00:00.000Z", "2023-09-30T00:00:00.000Z", PermissionStatus.FULFILLED),
                             Arguments.of(per, "2023-09-11T00:00:00.000Z", "2023-10-31T00:00:00.000Z", PermissionStatus.STREAMING_DATA),
                             Arguments.of(per,
                                          "2023-10-12T00:00:00.000Z",
                                          "2023-10-15T00:00:00.000Z",
                                          PermissionStatus.WAITING_FOR_START));
        }

        @BeforeEach
        void setUp() {
            var permissionScheduler = new PermissionScheduler(mockClock,
                                                              mockScheduler,
                                                              mockPermissionRepository,
                                                              new ConcurrentHashMap<>(),
                                                              streamerManager);
            service = new PermissionService(mockPermissionRepository,
                                            mockClock,
                                            streamerManager,
                                            mockHandshakeService,
                                            permissionScheduler,
                                            mockAuthService,
                                            mockAiidaLocalDataNeedService,
                                            mockAiidaEventPublisher);
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
            when(mockPermissionRepository.findAllActivePermissions()).thenReturn(List.of(permission));

            // strict stubbing requires us to only stub the .schedule if a runnable will be scheduled
            if (expectedState == PermissionStatus.WAITING_FOR_START || expectedState == PermissionStatus.STREAMING_DATA)
                doReturn(mockScheduledFuture).when(mockScheduler).schedule(any(), any(Instant.class));

            // When
            service.onApplicationEvent(mock(ContextRefreshedEvent.class));

            // Then
            assertEquals(expectedState, permission.status());
        }
    }
}
