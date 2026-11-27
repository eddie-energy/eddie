// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services;

import energy.eddie.aiida.dtos.permission.AcceptInboundPermissionDto;
import energy.eddie.aiida.dtos.permission.AcceptOutboundPermissionDto;
import energy.eddie.aiida.dtos.permission.PermissionDetailsDto;
import energy.eddie.aiida.dtos.permission.RejectPermissionDto;
import energy.eddie.aiida.errors.auth.InvalidUserException;
import energy.eddie.aiida.errors.datasource.DataSourceNotFoundException;
import energy.eddie.aiida.errors.datasource.IncompatibleDataSourceException;
import energy.eddie.aiida.errors.inbound.MissingMqttStreamingConfigException;
import energy.eddie.aiida.errors.permission.*;
import energy.eddie.aiida.models.connectionlimit.ConnectionLimitDefault;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.permission.*;
import energy.eddie.aiida.models.permission.dataneed.InboundAiidaLocalDataNeed;
import energy.eddie.aiida.models.permission.dataneed.OutboundAiidaLocalDataNeed;
import energy.eddie.aiida.repositories.ConnectionLimitDefaultRepository;
import energy.eddie.aiida.repositories.InboundPermissionRepository;
import energy.eddie.aiida.repositories.OutboundPermissionRepository;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.services.secrets.SecretsService;
import energy.eddie.api.agnostic.aiida.*;
import energy.eddie.api.agnostic.aiida.mqtt.MqttDto;
import energy.eddie.api.agnostic.process.model.PermissionStateTransitionException;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.dataneeds.needs.aiida.InboundAiidaDataNeed;
import energy.eddie.dataneeds.needs.aiida.OutboundAiidaDataNeed;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.web.client.HttpClientErrorException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static energy.eddie.aiida.config.AiidaConfiguration.AIIDA_ZONE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {
    private final UUID eddieId = UUID.fromString("a69f9bc2-e16c-4de4-8c3e-00d219dcd819");
    private final UUID permissionId1 = UUID.fromString("72831e2c-a01c-41b8-9db6-3f51670df7a5");
    private final UUID permissionId2 = UUID.fromString("72831e2c-a01c-41b8-9db6-3f51670df7a6");
    private final UUID dataNeedId = UUID.fromString("82831e2c-a01c-41b8-9db6-3f51670df7a5");
    private final UUID dataSourceId = UUID.fromString("92831e2c-a01c-41b8-9db6-3f51670df7a5");
    private final String handshakeUrl = "https://example.org/permission-request/{permissionId}";
    private final String accessToken = "accessToken";
    private final String connectionId = "NewAiidaRandomConnectionId";
    private final String meterId = "003114735";
    private final BigDecimal minLimitKw = BigDecimal.ONE;
    private final BigDecimal maxLimitKw = BigDecimal.TEN;
    private final LocalDate start = LocalDate.now(AIIDA_ZONE_ID);
    private final LocalDate end = start.plusDays(90);
    private final Instant fixedInstant = Instant.parse("2023-09-11T22:00:00.00Z");
    private final Clock clock = Clock.fixed(fixedInstant, AIIDA_ZONE_ID);
    private final UUID userId = UUID.randomUUID();
    private final AiidaPermissionRequestsDto permissionRequests =
            new AiidaPermissionRequestsDto(eddieId,
                                           List.of(permissionId1, permissionId2),
                                           handshakeUrl,
                                           accessToken);
    @Mock
    private PermissionRepository mockPermissionRepository;
    @Mock
    private InboundPermissionRepository mockInboundPermissionRepository;
    @Mock
    private OutboundPermissionRepository mockOutboundPermissionRepository;
    @Mock
    private ConnectionLimitDefaultRepository mockConnectionLimitDefaultRepository;
    @Mock
    private PermissionScheduler mockPermissionScheduler;
    @Mock
    private HandshakeService mockHandshakeService;
    @Mock
    private OutboundAiidaDataNeed mockDataNeed;
    @Mock
    private PermissionDetailsDto mockDetails;
    @Mock
    private InboundAiidaLocalDataNeed mockInboundAiidaLocalDataNeed;
    @Mock
    private AuthService mockAuthService;
    @Mock
    private DataSourceService mockDataSourceService;
    @Mock
    private Permission mockPermission;
    @Mock
    private InboundPermission mockInboundPermission;
    @Mock
    private OutboundPermission mockOutboundPermission;
    @Mock
    private DataSource mockDataSource;
    @Mock
    private InboundDataSource mockInboundDataSource;
    @Mock
    private MqttDto mockMqttDto;
    @Mock
    private AiidaLocalDataNeedService mockAiidaLocalDataNeedService;
    @Mock
    private SecretsService mockSecretsService;
    @Captor
    private ArgumentCaptor<Permission> permissionCaptor;
    @Captor
    private ArgumentCaptor<ConnectionLimitDefault> connectionLimitDefaultCaptor;
    private PermissionService service;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(mockDataSourceService.dataSourceByIdOrThrow(any())).thenReturn(mockDataSource);
        lenient().when(mockDataSource.userId()).thenReturn(userId);
        lenient().when(mockPermission.userId()).thenReturn(userId);

        service = new PermissionService(mockPermissionRepository,
                                        mockInboundPermissionRepository,
                                        mockOutboundPermissionRepository,
                                        mockConnectionLimitDefaultRepository,
                                        clock,
                                        mockHandshakeService,
                                        mockPermissionScheduler,
                                        mockAuthService,
                                        mockDataSourceService,
                                        mockAiidaLocalDataNeedService,
                                        mockSecretsService);
    }

    @Test
    void givenNotExistingPermission_findById_throws() {
        var notExistingId = UUID.fromString("12831e2c-a01c-41b8-9db6-3f51670df7a5");

        when(mockPermissionRepository.findById(notExistingId)).thenReturn(Optional.empty());

        assertThrows(PermissionNotFoundException.class, () -> service.findById(notExistingId));
    }

    @Test
    void givenExistingPermissionId_fetchPermissionDetails_throws() throws InvalidUserException {
        // Given
        when(mockPermissionRepository.existsById(permissionId1)).thenReturn(true);
        when(mockAuthService.getCurrentUserId()).thenReturn(userId);

        // When, Then
        assertThrows(PermissionAlreadyExistsException.class, () -> service.fetchPermissionDetails(permissionRequests));
        verifyNoInteractions(mockHandshakeService);
    }

    @Test
    void givenExceptionFromHandshakeService_fetchPermissionDetails_throwsDetailFetchingFailedException() throws Exception {
        // Given
        var exception = HttpClientErrorException.create(HttpStatus.GONE,
                                                        "Gone",
                                                        HttpHeaders.EMPTY,
                                                        "".getBytes(StandardCharsets.UTF_8),
                                                        StandardCharsets.UTF_8);
        when(mockHandshakeService.fetchDetails(any())).thenReturn(Mono.error(exception));
        when(mockAuthService.getCurrentUserId()).thenReturn(userId);

        // When, Then
        assertThrows(DetailFetchingFailedException.class, () -> service.fetchPermissionDetails(permissionRequests));
        verify(mockPermissionRepository, never()).save(any());
    }

    @Test
    void givenValidDto_fetchPermissionDetails_returnsDetails_withoutPersisting() throws Exception {
        // Given
        var expectedStart = ZonedDateTime.of(start, LocalTime.MIN, AIIDA_ZONE_ID).toInstant();
        var expectedEnd = ZonedDateTime.of(end, LocalTime.MAX.withNano(0), AIIDA_ZONE_ID).toInstant();
        stubOutboundDataNeed();
        var details = outboundDetails(start, null, null);
        when(mockHandshakeService.fetchDetails(any())).thenReturn(Mono.just(details));
        when(mockAuthService.getCurrentUserId()).thenReturn(userId);

        // When
        var pendingPermissions = service.fetchPermissionDetails(permissionRequests);

        // Then
        verify(mockHandshakeService).fetchDetails(argThat(target -> target.handshakeUrl().endsWith("/" + permissionId1) && target.accessToken().equals(accessToken)));
        verify(mockHandshakeService).fetchDetails(argThat(target -> target.handshakeUrl().endsWith("/" + permissionId2)));
        verify(mockPermissionRepository, never()).save(any());
        verifyNoInteractions(mockConnectionLimitDefaultRepository);

        assertEquals(2, pendingPermissions.size());
        var pending = pendingPermissions.getFirst();
        assertEquals(permissionId1, pending.permissionId());
        assertEquals(eddieId, pending.eddieId());
        assertEquals(meterId, pending.meterId());
        assertEquals(expectedStart, pending.startTime());
        assertEquals(expectedEnd, pending.expirationTime());
        assertEquals("My Name", pending.displayName());

        var dataNeed = pending.dataNeed();
        assertEquals(dataNeedId, dataNeed.dataNeedId());
        assertEquals("*/23 * * * * *", dataNeed.transmissionSchedule().toString());
        assertEquals(OutboundAiidaDataNeed.DISCRIMINATOR_VALUE, dataNeed.type());
        assertThat(((OutboundAiidaLocalDataNeed) dataNeed).dataTags()).hasSameElementsAs(Set.of(ObisCode.POSITIVE_ACTIVE_ENERGY));
        assertThat(dataNeed.schemas()).hasSameElementsAs(Set.of(AiidaSchema.SMART_METER_P1_RAW));
    }

    @Test
    void givenLimitDefaultsButIneligibleDataNeed_fetchPermissionDetails_denotesUnfulfillableAndThrows() throws Exception {
        // Given
        stubOutboundDataNeed();
        var details = outboundDetails(start, minLimitKw, maxLimitKw);
        when(mockHandshakeService.fetchDetails(any())).thenReturn(Mono.just(details));
        when(mockAuthService.getCurrentUserId()).thenReturn(userId);

        // When, Then
        assertThrows(LimitDefaultsNotAllowedException.class, () -> service.fetchPermissionDetails(permissionRequests));
        verify(mockHandshakeService).denoteUnfulfillable(any(), notNull());
        verify(mockPermissionRepository, never()).save(any());
        verifyNoInteractions(mockConnectionLimitDefaultRepository);
    }

    @Test
    void givenInvalidDataNeedType_fetchPermissionDetails_denotesUnfulfillableAndThrows() throws Exception {
        // Given
        stubOutboundDataNeed();
        when(mockDataNeed.type()).thenReturn("illegalValue");
        var details = outboundDetails(start, null, null);
        when(mockHandshakeService.fetchDetails(any())).thenReturn(Mono.just(details));
        when(mockAuthService.getCurrentUserId()).thenReturn(userId);

        // When, Then
        var exception = assertThrows(PermissionDataNeedTypeNotSupportedException.class,
                                     () -> service.fetchPermissionDetails(permissionRequests));
        verify(mockHandshakeService).denoteUnfulfillable(argThat(target -> target.handshakeUrl().endsWith("/" + permissionId1)),
                                                         eq(exception.getMessage()));
        verify(mockPermissionRepository, never()).save(any());
    }

    @Test
    void givenStartDateInThePast_fetchPermissionDetails_denotesUnfulfillableAndThrows() throws Exception {
        // Given
        stubOutboundDataNeed();
        var details = outboundDetails(start.minusDays(1), null, null);
        when(mockHandshakeService.fetchDetails(any())).thenReturn(Mono.just(details));
        when(mockAuthService.getCurrentUserId()).thenReturn(userId);

        // When, Then
        assertThrows(PermissionStartInThePastException.class, () -> service.fetchPermissionDetails(permissionRequests));
        verify(mockHandshakeService).denoteUnfulfillable(any(), notNull());
        verify(mockPermissionRepository, never()).save(any());
    }

    @Test
    void givenFcaPermissionWithMeterIdAndActiveDuplicate_fetchPermissionDetails_denotesUnfulfillableAndThrows() throws Exception {
        // Given
        stubOutboundDataNeed();
        when(mockDataNeed.contexts()).thenReturn(Set.of(AiidaContext.FLEXIBLE_CONNECTION_AGREEMENT));
        var details = outboundDetails(start, null, null);
        when(mockHandshakeService.fetchDetails(any())).thenReturn(Mono.just(details));
        when(mockAuthService.getCurrentUserId()).thenReturn(userId);
        when(mockPermissionRepository.existsByUserIdAndDataNeedTypeAndContextAndMeterIdAndStatus(
                userId,
                OutboundAiidaDataNeed.DISCRIMINATOR_VALUE,
                AiidaContext.FLEXIBLE_CONNECTION_AGREEMENT,
                meterId,
                PermissionStatus.ACCEPTED)
        ).thenReturn(true);

        // When, Then
        var exception = assertThrows(ActiveFcaPermissionAlreadyExistsException.class,
                                     () -> service.fetchPermissionDetails(permissionRequests));
        assertThat(exception.getMessage()).contains("outbound").contains(meterId);
        verify(mockHandshakeService).denoteUnfulfillable(any(), eq(exception.getMessage()));
        verify(mockPermissionRepository, never()).save(any());
    }

    @Test
    void givenValidOutboundPermission_acceptOutboundPermission_persistsAcceptedPermission_andStartsIt() throws Exception {
        // Given
        stubOutboundDataNeed();
        stubSuccessfulAccept();
        when(mockDataSource.asset()).thenReturn(AiidaAsset.CONNECTION_AGREEMENT_POINT);

        // When
        service.acceptOutboundPermission(permissionId1, outboundAcceptDto(dataSourceId, "My display name"));

        // Then
        verify(mockHandshakeService).accept(argThat(target -> target.handshakeUrl().endsWith("/" + permissionId1)));
        verify(mockPermissionRepository).save(permissionCaptor.capture());
        var permission = permissionCaptor.getValue();
        assertEquals(permissionId1, permission.id());
        assertEquals(eddieId, permission.eddieId());
        assertEquals(PermissionStatus.ACCEPTED, permission.status());
        assertEquals(fixedInstant, permission.grantTime());
        assertEquals(mockDataSource, permission.dataSource());
        assertEquals("My display name", permission.displayName());
        assertEquals(connectionId, permission.connectionId());
        var mqttStreamingConfig = permission.mqttStreamingConfig();
        assertNotNull(mqttStreamingConfig);
        assertEquals("dataTopic", mqttStreamingConfig.dataTopic());
        verify(mockPermissionScheduler).scheduleOrStart(argThat(acceptedPermission -> acceptedPermission.id().equals(permissionId1)));
        verifyNoInteractions(mockConnectionLimitDefaultRepository);
    }

    @Test
    void givenInboundPermission_acceptOutboundPermission_throws() throws Exception {
        // Given
        stubInboundLocalDataNeed(false);
        var details = outboundDetails(start, null, null);
        when(mockHandshakeService.fetchDetails(any())).thenReturn(Mono.just(details));
        when(mockAuthService.getCurrentUserId()).thenReturn(userId);

        // When, Then
        assertThrows(InvalidOutboundPermissionException.class,
                     () -> service.acceptOutboundPermission(permissionId1, outboundAcceptDto(dataSourceId, null)));
        verify(mockHandshakeService, never()).accept(any());
        verify(mockPermissionRepository, never()).save(any());
    }

    @Test
    void givenOutboundPermission_acceptInboundPermission_throws() throws Exception {
        // Given
        stubOutboundDataNeed();
        var details = outboundDetails(start, null, null);
        when(mockHandshakeService.fetchDetails(any())).thenReturn(Mono.just(details));
        when(mockAuthService.getCurrentUserId()).thenReturn(userId);

        // When, Then
        assertThrows(InvalidInboundPermissionException.class,
                     () -> service.acceptInboundPermission(permissionId1,
                                                           inboundAcceptDto(InboundMessageFormat.OPENADR_3_1)));
        verify(mockHandshakeService, never()).accept(any());
        verify(mockPermissionRepository, never()).save(any());
    }

    @Test
    void givenDataSourceOfOtherUser_acceptOutboundPermission_throwsDataSourceNotFound() throws Exception {
        // Given
        stubOutboundDataNeed();
        var details = outboundDetails(start, null, null);
        when(mockHandshakeService.fetchDetails(any())).thenReturn(Mono.just(details));
        when(mockAuthService.getCurrentUserId()).thenReturn(userId);
        when(mockDataSource.userId()).thenReturn(UUID.randomUUID());

        // When, Then
        assertThrows(DataSourceNotFoundException.class,
                     () -> service.acceptOutboundPermission(permissionId1, outboundAcceptDto(dataSourceId, null)));
        verify(mockHandshakeService, never()).accept(any());
    }

    @Test
    void givenDataSourceWithOtherAsset_acceptOutboundPermission_throwsIncompatibleDataSource() throws Exception {
        // Given
        stubOutboundDataNeed();
        var details = outboundDetails(start, null, null);
        when(mockHandshakeService.fetchDetails(any())).thenReturn(Mono.just(details));
        when(mockAuthService.getCurrentUserId()).thenReturn(userId);
        when(mockDataSource.asset()).thenReturn(AiidaAsset.DEDICATED_MEASUREMENT_DEVICE);

        // When, Then
        var exception = assertThrows(IncompatibleDataSourceException.class,
                                     () -> service.acceptOutboundPermission(permissionId1, outboundAcceptDto(dataSourceId, null)));
        assertThat(exception.getMessage()).contains("DEDICATED-MEASUREMENT-DEVICE")
                                          .contains("CONNECTION-AGREEMENT-POINT");
        verify(mockHandshakeService, never()).accept(any());
    }

    @Test
    void givenInboundDataSourceWithIncompatibleSchemas_acceptOutboundPermission_throwsIncompatibleDataSource() throws Exception {
        // Given
        stubOutboundDataNeed();
        var details = outboundDetails(start, null, null);
        when(mockHandshakeService.fetchDetails(any())).thenReturn(Mono.just(details));
        when(mockAuthService.getCurrentUserId()).thenReturn(userId);
        when(mockDataSourceService.dataSourceByIdOrThrow(dataSourceId)).thenReturn(mockInboundDataSource);
        when(mockInboundDataSource.userId()).thenReturn(userId);
        when(mockInboundDataSource.asset()).thenReturn(AiidaAsset.CONNECTION_AGREEMENT_POINT);
        when(mockInboundDataSource.schemas()).thenReturn(Set.of(AiidaSchema.OPAQUE));

        // When, Then
        assertThrows(IncompatibleDataSourceException.class,
                     () -> service.acceptOutboundPermission(permissionId1, outboundAcceptDto(dataSourceId, null)));
        verify(mockHandshakeService, never()).accept(any());
    }

    @Test
    void givenInboundDataSourceWithPartialSchemaOverlap_acceptOutboundPermission_succeeds() throws Exception {
        // Given
        stubOutboundDataNeed();
        stubSuccessfulAccept();
        when(mockDataSourceService.dataSourceByIdOrThrow(dataSourceId)).thenReturn(mockInboundDataSource);
        when(mockInboundDataSource.userId()).thenReturn(userId);
        when(mockInboundDataSource.asset()).thenReturn(AiidaAsset.CONNECTION_AGREEMENT_POINT);
        when(mockInboundDataSource.schemas()).thenReturn(Set.of(AiidaSchema.SMART_METER_P1_RAW, AiidaSchema.OPAQUE));

        // When
        service.acceptOutboundPermission(permissionId1, outboundAcceptDto(dataSourceId, null));

        // Then
        verify(mockPermissionRepository).save(permissionCaptor.capture());
        assertEquals(mockInboundDataSource, permissionCaptor.getValue().dataSource());
    }

    @Test
    void givenExceptionFromHandshakeService_acceptOutboundPermission_throws_withoutPersisting() throws Exception {
        // Given
        stubOutboundDataNeed();
        var details = outboundDetails(start, null, null);
        when(mockHandshakeService.fetchDetails(any())).thenReturn(Mono.just(details));
        when(mockAuthService.getCurrentUserId()).thenReturn(userId);
        when(mockDataSource.asset()).thenReturn(AiidaAsset.CONNECTION_AGREEMENT_POINT);
        var exception = HttpClientErrorException.create(HttpStatus.GONE,
                                                        "Gone",
                                                        HttpHeaders.EMPTY,
                                                        "".getBytes(StandardCharsets.UTF_8),
                                                        StandardCharsets.UTF_8);
        when(mockHandshakeService.accept(any())).thenReturn(Mono.error(exception));

        // When, Then
        assertThrows(DetailFetchingFailedException.class,
                     () -> service.acceptOutboundPermission(permissionId1, outboundAcceptDto(dataSourceId, null)));
        verify(mockPermissionRepository, never()).save(any());
        verifyNoInteractions(mockPermissionScheduler);
    }

    @Test
    void givenInboundPermission_acceptInboundPermission_persistsFormat_andCreatesInboundDataSource() throws Exception {
        // Given
        stubInboundLocalDataNeed(false);
        stubSuccessfulAccept();
        when(mockDataSourceService.createInboundDataSource(any())).thenReturn(mockInboundDataSource);

        // When
        service.acceptInboundPermission(permissionId1, inboundAcceptDto(InboundMessageFormat.OPENADR_3_1));

        // Then
        verify(mockDataSourceService, never()).dataSourceByIdOrThrow(any());
        verify(mockPermissionRepository).save(permissionCaptor.capture());
        var permission = permissionCaptor.getValue();
        assertEquals(InboundMessageFormat.OPENADR_3_1, ((InboundPermission) permission).inboundMessageFormat());
        assertEquals(mockInboundDataSource, permission.dataSource());
        verify(mockDataSourceService).startDataSource(mockInboundDataSource);
        verify(mockPermissionScheduler).scheduleOrStart(argThat(acceptedPermission -> acceptedPermission.id().equals(permissionId1)));
    }

    @Test
    void givenInboundDataSourceCreationFails_acceptInboundPermission_doesNotStartDataSource() throws Exception {
        // Given
        stubInboundLocalDataNeed(false);
        stubSuccessfulAccept();
        when(mockDataSourceService.createInboundDataSource(any()))
                .thenThrow(new MissingMqttStreamingConfigException(null));
        var acceptDto = inboundAcceptDto(null);

        // When, Then
        assertThrows(IllegalStateException.class, () -> service.acceptInboundPermission(permissionId1, acceptDto));
        verify(mockDataSourceService, never()).startDataSource(any());
        verifyNoInteractions(mockPermissionScheduler);
    }

    @Test
    void givenLimitDefaultsAndEligibleDataNeed_acceptInboundPermission_persistsLimitDefaults() throws Exception {
        // Given
        stubInboundLocalDataNeed(true);
        stubSuccessfulAccept(minLimitKw, maxLimitKw);
        when(mockDataSourceService.createInboundDataSource(any())).thenReturn(mockInboundDataSource);

        // When
        service.acceptInboundPermission(permissionId1, inboundAcceptDto(null));

        // Then
        // The default is persisted through the default repository, not through the permission.
        var inOrder = inOrder(mockPermissionRepository, mockConnectionLimitDefaultRepository);
        inOrder.verify(mockPermissionRepository).save(any());
        inOrder.verify(mockConnectionLimitDefaultRepository).save(connectionLimitDefaultCaptor.capture());
        var defaultLimit = connectionLimitDefaultCaptor.getValue();
        assertEquals(permissionId1, defaultLimit.permissionId());
        assertEquals(meterId, defaultLimit.meterId());
        assertEquals(clock.instant(), defaultLimit.start());
        assertNull(defaultLimit.end());
        assertThat(defaultLimit.minLimitKw()).isEqualByComparingTo(minLimitKw);
        assertThat(defaultLimit.maxLimitKw()).isEqualByComparingTo(maxLimitKw);
    }

    @Test
    void givenAlreadyAcceptedPermission_acceptOutboundPermission_throws() throws Exception {
        // Given
        when(mockPermissionRepository.existsById(permissionId1)).thenReturn(true);
        when(mockAuthService.getCurrentUserId()).thenReturn(userId);

        // When, Then
        assertThrows(PermissionAlreadyExistsException.class,
                     () -> service.acceptOutboundPermission(permissionId1, outboundAcceptDto(dataSourceId, null)));
        verifyNoInteractions(mockHandshakeService);
    }

    @Test
    void givenPermissionRequest_rejectPermission_rejectsAtEddie_withoutPersisting() throws Exception {
        // When
        service.rejectPermission(permissionId1, new RejectPermissionDto(handshakeUrl, accessToken, "Not interested"));

        // Then
        verify(mockHandshakeService).reject(argThat(target -> target.handshakeUrl().endsWith("/" + permissionId1)
                                                              && target.accessToken().equals(accessToken)),
                                            eq("Not interested"));
        verify(mockPermissionRepository, never()).save(any());
    }

    @Test
    void givenAlreadyAcceptedPermission_rejectPermission_throws() {
        // Given
        when(mockPermissionRepository.existsById(permissionId1)).thenReturn(true);

        // When, Then
        assertThrows(PermissionAlreadyExistsException.class,
                     () -> service.rejectPermission(permissionId1,
                                                    new RejectPermissionDto(handshakeUrl, accessToken, null)));
        verifyNoInteractions(mockHandshakeService);
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
        when(mockPermissionRepository.findByUserIdOrderByGrantTimeDescRevokeTimeDesc(uuid)).thenReturn(List.of(mockPermission));

        // When
        var result = service.getAllPermissionsSortedByGrantTime();

        // Then
        assertEquals(List.of(mockPermission), result);
    }

    @Test
    void givenValidUser_getAcceptedInboundPermissions() throws InvalidUserException {
        // Given
        var uuid = UUID.fromString("dc9ff3d3-1f1f-445d-a4ee-85c1faffb715");
        when(mockAuthService.getCurrentUserId()).thenReturn(uuid);
        when(mockInboundPermissionRepository.findByUserIdAndDataSourceIsNotNullAndStatusOrderByGrantTimeDesc(uuid,
                                                                                                           PermissionStatus.ACCEPTED))
                .thenReturn(List.of(mockInboundPermission));

        // When
        var result = service.getAcceptedInboundPermissions();

        // Then
        assertEquals(List.of(mockInboundPermission), result);
    }

    @Test
    void givenPermissionInInvalidState_revokePermission_throws() {
        // Given
        when(mockPermissionRepository.findById(permissionId1)).thenReturn(Optional.of(mockPermission));
        when(mockPermission.id()).thenReturn(permissionId1);
        when(mockPermission.status()).thenReturn(PermissionStatus.FULFILLED);

        // When, Then
        assertThrows(PermissionStateTransitionException.class, () -> service.revokePermission(permissionId1));
    }

    @Test
    void givenValidPermission_revokePermission_updatesState_andRemovesScheduledRunnables() throws Exception {
        // Given
        when(mockPermissionRepository.findById(permissionId1)).thenReturn(Optional.of(mockOutboundPermission));
        when(mockOutboundPermission.status()).thenReturn(PermissionStatus.ACCEPTED);
        when(mockOutboundPermission.id()).thenReturn(permissionId1);

        // When
        service.revokePermission(permissionId1);

        // Then
        verify(mockOutboundPermission).setStatus(PermissionStatus.REVOKED);
        verify(mockOutboundPermission).setRevokeTime(fixedInstant);
        verify(mockPermissionScheduler).stopPermission(mockOutboundPermission, PermissionProcessStatus.REVOKED);
    }

    @Test
    void givenValidInboundPermission_revokePermission_updatesState_andRemovesScheduledRunnables() throws Exception {
        // Given
        when(mockPermissionRepository.findById(permissionId1)).thenReturn(Optional.of(mockInboundPermission));
        when(mockInboundPermission.status()).thenReturn(PermissionStatus.ACCEPTED);
        when(mockInboundPermission.dataSource()).thenReturn(mockInboundDataSource);
        when(mockInboundPermission.id()).thenReturn(permissionId1);
        when(mockInboundDataSource.permission()).thenReturn(mockInboundPermission);
        when(mockInboundDataSource.id()).thenReturn(UUID.randomUUID());

        // When
        service.revokePermission(permissionId1);

        // Then
        verify(mockInboundPermission).setStatus(PermissionStatus.REVOKED);
        verify(mockInboundPermission).setRevokeTime(fixedInstant);
        verify(mockPermissionScheduler).stopPermission(mockInboundPermission, PermissionProcessStatus.REVOKED);
    }

    @Test
    void givenOutboundPermissionUsingInboundDataSource_revokePermission_doesNotDeleteInboundDataSource() throws Exception {
        // Given
        when(mockPermissionRepository.findById(permissionId1)).thenReturn(Optional.of(mockOutboundPermission));
        when(mockOutboundPermission.status()).thenReturn(PermissionStatus.ACCEPTED);
        when(mockOutboundPermission.id()).thenReturn(permissionId1);
        lenient().when(mockOutboundPermission.dataSource()).thenReturn(mockInboundDataSource);

        // When
        service.revokePermission(permissionId1);

        // Then
        verify(mockOutboundPermission, never()).setDataSource(any());
    }

    @Test
    void givenInboundPermissionWithBlockingOutboundPermissions_revokePermission_throws() {
        var blockingPermissionId1 = UUID.fromString("a57a9f3d-2b5a-4b84-a32c-51bfa4b865c1");
        var blockingPermissionId2 = UUID.fromString("6dbc4ec9-4f2f-4aa7-b016-36c28945a6e4");
        var blockingPermission1 = mock(OutboundPermission.class);
        when(blockingPermission1.id()).thenReturn(blockingPermissionId1);
        var blockingPermission2 = mock(OutboundPermission.class);
        when(blockingPermission2.id()).thenReturn(blockingPermissionId2);

        when(mockPermissionRepository.findById(permissionId1)).thenReturn(Optional.of(mockInboundPermission));
        when(mockInboundPermission.id()).thenReturn(permissionId1);
        when(mockInboundPermission.status()).thenReturn(PermissionStatus.ACCEPTED);
        when(mockInboundPermission.dataSource()).thenReturn(mockInboundDataSource);
        when(mockInboundDataSource.permission()).thenReturn(mockInboundPermission);
        when(mockInboundDataSource.id()).thenReturn(dataSourceId);
        when(mockOutboundPermissionRepository.findByDataSourceIdAndStatus(dataSourceId, PermissionStatus.ACCEPTED))
                .thenReturn(List.of(blockingPermission1, blockingPermission2));

        var exception = assertThrows(InboundDataSourceInUseException.class,
                                     () -> service.revokePermission(permissionId1));

        assertTrue(exception.getMessage().contains(blockingPermissionId1.toString()));
        assertTrue(exception.getMessage().contains(blockingPermissionId2.toString()));
        verify(mockPermissionScheduler, never()).stopPermission(any(), any());
    }

    @Test
    void givenInboundPermission_updateInboundMessageFormat_updatesPermission() throws Exception {
        // Given
        when(mockInboundPermissionRepository.findById(permissionId1)).thenReturn(Optional.of(mockInboundPermission));

        // When
        var updatedPermission = service.updateInboundMessageFormat(permissionId1, InboundMessageFormat.OPENADR_3_1);

        // Then
        assertEquals(mockInboundPermission, updatedPermission);
        verify(mockInboundPermission).updateInboundMessageFormat(InboundMessageFormat.OPENADR_3_1);
    }

    @Test
    void givenNoInboundPermission_updateInboundMessageFormat_throwsNotFound() {
        when(mockInboundPermissionRepository.findById(permissionId1)).thenReturn(Optional.empty());

        assertThrows(PermissionNotFoundException.class,
                     () -> service.updateInboundMessageFormat(permissionId1, InboundMessageFormat.OPENADR_3_1));
    }

    @Test
    void givenDisplayName_updateDisplayName_updatesPermission() throws Exception {
        // Given
        when(mockPermissionRepository.findById(permissionId1)).thenReturn(Optional.of(mockPermission));

        // When
        var updatedPermission = service.updateDisplayName(permissionId1, "My smart meter");

        // Then
        assertEquals(mockPermission, updatedPermission);
        verify(mockPermission).setDisplayName("My smart meter");
    }

    @Test
    void givenBlankDisplayName_updateDisplayName_throws() {
        assertThrows(MissingDisplayNameException.class,
                     () -> service.updateDisplayName(permissionId1, "  "));

        verifyNoInteractions(mockPermissionRepository);
    }

    @Test
    void givenNullDisplayName_updateDisplayName_throws() {
        assertThrows(MissingDisplayNameException.class,
                     () -> service.updateDisplayName(permissionId1, null));

        verifyNoInteractions(mockPermissionRepository);
    }

    private PermissionDetailsDto outboundDetails(
            LocalDate permissionStart,
            @Nullable BigDecimal minLimit,
            @Nullable BigDecimal maxLimit
    ) {
        lenient().when(mockDetails.permissionId()).thenReturn(permissionId1);
        lenient().when(mockDetails.connectionId()).thenReturn(connectionId);
        lenient().when(mockDetails.meterId()).thenReturn(meterId);
        lenient().when(mockDetails.minLimitKw()).thenReturn(minLimit);
        lenient().when(mockDetails.maxLimitKw()).thenReturn(maxLimit);
        lenient().when(mockDetails.start()).thenReturn(permissionStart);
        lenient().when(mockDetails.end()).thenReturn(end);
        lenient().when(mockDetails.dataNeed()).thenReturn(mockDataNeed);
        return mockDetails;
    }

    private void stubOutboundDataNeed() {
        lenient().when(mockDataNeed.transmissionSchedule()).thenReturn(CronExpression.parse("*/23 * * * * *"));
        lenient().when(mockDataNeed.dataNeedId()).thenReturn(dataNeedId);
        lenient().when(mockDataNeed.type()).thenReturn(OutboundAiidaDataNeed.DISCRIMINATOR_VALUE);
        lenient().when(mockDataNeed.name()).thenReturn("My Name");
        lenient().when(mockDataNeed.purpose()).thenReturn("Some purpose");
        lenient().when(mockDataNeed.policyLink()).thenReturn("https://example.org");
        lenient().when(mockDataNeed.asset()).thenReturn(AiidaAsset.CONNECTION_AGREEMENT_POINT);
        lenient().when(mockDataNeed.dataTags()).thenReturn(Set.of(ObisCode.POSITIVE_ACTIVE_ENERGY));
        lenient().when(mockDataNeed.contexts()).thenReturn(Set.of());
        lenient().when(mockDataNeed.schemas()).thenReturn(Set.of(AiidaSchema.SMART_METER_P1_RAW));
    }

    private void stubInboundLocalDataNeed(boolean supportsConnectionLimits) {
        lenient().when(mockDataNeed.dataNeedId()).thenReturn(dataNeedId);
        lenient().when(mockDataNeed.type()).thenReturn(InboundAiidaDataNeed.DISCRIMINATOR_VALUE);
        lenient().when(mockAiidaLocalDataNeedService.optionalAiidaLocalDataNeedById(dataNeedId))
                 .thenReturn(Optional.of(mockInboundAiidaLocalDataNeed));
        lenient().when(mockInboundAiidaLocalDataNeed.name()).thenReturn("My Name");
        lenient().when(mockInboundAiidaLocalDataNeed.type()).thenReturn(InboundAiidaDataNeed.DISCRIMINATOR_VALUE);
        lenient().when(mockInboundAiidaLocalDataNeed.supportsConnectionLimits()).thenReturn(supportsConnectionLimits);
        lenient().when(mockInboundAiidaLocalDataNeed.transmissionSchedule())
                 .thenReturn(CronExpression.parse("*/5 * * * * *"));
    }

    private void stubSuccessfulAccept() throws InvalidUserException {
        stubSuccessfulAccept(null, null);
    }

    private void stubSuccessfulAccept(
            @Nullable BigDecimal minLimit,
            @Nullable BigDecimal maxLimit
    ) throws InvalidUserException {
        var details = outboundDetails(start, minLimit, maxLimit);
        when(mockHandshakeService.fetchDetails(any())).thenReturn(Mono.just(details));
        when(mockAuthService.getCurrentUserId()).thenReturn(userId);
        when(mockHandshakeService.accept(any())).thenReturn(Mono.just(mockMqttDto));
        when(mockMqttDto.dataTopic()).thenReturn("dataTopic");
        when(mockMqttDto.username()).thenReturn(permissionId1.toString());
        lenient().when(mockPermissionRepository.save(any(Permission.class))).then(i -> i.getArgument(0));
    }

    private AcceptOutboundPermissionDto outboundAcceptDto(UUID selectedDataSourceId, @Nullable String displayName) {
        return new AcceptOutboundPermissionDto(eddieId, handshakeUrl, accessToken, selectedDataSourceId, displayName);
    }

    private AcceptInboundPermissionDto inboundAcceptDto(@Nullable InboundMessageFormat inboundMessageFormat) {
        return new AcceptInboundPermissionDto(eddieId, handshakeUrl, accessToken, inboundMessageFormat, null);
    }
}
