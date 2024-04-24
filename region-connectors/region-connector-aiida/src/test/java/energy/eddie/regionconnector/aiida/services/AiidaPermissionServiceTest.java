package energy.eddie.regionconnector.aiida.services;

import energy.eddie.api.agnostic.process.model.PermissionStateTransitionException;
import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.needs.aiida.GenericAiidaDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.dataneeds.utils.DataNeedWrapper;
import energy.eddie.regionconnector.aiida.config.PlainAiidaConfiguration;
import energy.eddie.regionconnector.aiida.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.aiida.exceptions.CredentialsAlreadyExistException;
import energy.eddie.regionconnector.aiida.mqtt.MqttDto;
import energy.eddie.regionconnector.aiida.mqtt.MqttService;
import energy.eddie.regionconnector.aiida.permission.request.AiidaPermissionRequest;
import energy.eddie.regionconnector.aiida.permission.request.events.MqttCredentialsCreatedEvent;
import energy.eddie.regionconnector.aiida.permission.request.persistence.AiidaPermissionRequestViewRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static energy.eddie.regionconnector.aiida.AiidaRegionConnectorMetadata.REGION_CONNECTOR_ZONE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiidaPermissionServiceTest {
    private final String connectionId = "testConnId";
    private final String dataNeedId = "testDataNeedId";
    @Mock
    private DataNeedsService mockDataNeedsService;
    @Mock
    private Outbox mockOutbox;
    @Mock
    private GenericAiidaDataNeed mockDataNeed;
    @Mock
    private ValidatedHistoricalDataDataNeed unsupportedDataNeed;
    @Mock
    private RelativeDuration mockRelativeDuration;
    @Mock
    private AiidaPermissionRequestViewRepository mockViewRepository;
    @Mock
    private MqttService mockMqttService;
    @Mock
    private AiidaPermissionRequest mockRequest;
    @Mock
    private MqttDto mockMqttDto;
    @Captor
    private ArgumentCaptor<PermissionEvent> permissionEventCaptor;
    private AiidaPermissionService service;

    @BeforeEach
    void setUp() {
        PlainAiidaConfiguration config = new PlainAiidaConfiguration(
                "localhost:1234",
                "testData",
                "testStatus",
                "testTerminationPrefix",
                "customerId",
                4
        );
        var fixedClock = Clock.fixed(Instant.parse("2023-10-15T15:00:00Z"),
                                     REGION_CONNECTOR_ZONE_ID);
        service = new AiidaPermissionService(mockOutbox,
                                             mockDataNeedsService,
                                             fixedClock,
                                             config,
                                             mockMqttService,
                                             mockViewRepository);
    }

    @Test
    void givenNonExistingDataNeedId_createValidateAndSendPermissionRequest_throwsException() throws DataNeedNotFoundException {
        // Given
        var dataNeedId = "NonExisting";
        when(mockDataNeedsService.findDataNeedAndCalculateStartAndEnd(anyString(), any(), any(), any())).thenThrow(
                DataNeedNotFoundException.class);

        // Then
        assertThrows(DataNeedNotFoundException.class,
                     // When
                     () -> service.createValidateAndSendPermissionRequest(new PermissionRequestForCreation(
                             "testConnId", dataNeedId)));
    }

    @Test
    void givenUnsupportedDataNeed_createValidateAndSendPermissionRequest_throwsException() throws DataNeedNotFoundException {
        // Given
        DataNeedWrapper wrapper = new DataNeedWrapper(unsupportedDataNeed,
                                                      LocalDate.now(REGION_CONNECTOR_ZONE_ID),
                                                      LocalDate.now(REGION_CONNECTOR_ZONE_ID));
        when(mockDataNeedsService.findDataNeedAndCalculateStartAndEnd(any(), any(), any(), any())).thenReturn(wrapper);

        // When, Then
        assertThrows(UnsupportedDataNeedException.class,
                     () -> service.createValidateAndSendPermissionRequest(new PermissionRequestForCreation(connectionId,
                                                                                                           dataNeedId)));
    }

    @Test
    void givenOpenStartEndDataNeed_createValidateAndSendPermissionRequest_usesFixedValues() throws DataNeedNotFoundException, UnsupportedDataNeedException {
        // Given
        when(mockDataNeed.duration()).thenReturn(mockRelativeDuration);
        when(mockRelativeDuration.start()).thenReturn(Optional.empty());
        when(mockRelativeDuration.end()).thenReturn(Optional.empty());
        DataNeedWrapper wrapper = new DataNeedWrapper(mockDataNeed,
                                                      LocalDate.now(REGION_CONNECTOR_ZONE_ID),
                                                      LocalDate.now(REGION_CONNECTOR_ZONE_ID));
        when(mockDataNeedsService.findDataNeedAndCalculateStartAndEnd(any(), any(), any(), any())).thenReturn(wrapper);

        // When
        var dto = service.createValidateAndSendPermissionRequest(new PermissionRequestForCreation(connectionId,
                                                                                                  dataNeedId));

        // Then
        assertEquals(LocalDate.parse("2000-01-01"), LocalDate.ofInstant(dto.startTime(), REGION_CONNECTOR_ZONE_ID));
        assertEquals(LocalDate.parse("9999-12-31"),
                     LocalDate.ofInstant(dto.expirationTime(), REGION_CONNECTOR_ZONE_ID));
    }

    @Test
    void givenRelativeDuration_createValidateAndSendPermissionRequest_usesCalculatedStartAndEnd() throws DataNeedNotFoundException, UnsupportedDataNeedException {
        // Given
        when(mockDataNeed.duration()).thenReturn(mockRelativeDuration);
        when(mockDataNeed.name()).thenReturn("FooBar");
        when(mockDataNeed.id()).thenReturn(dataNeedId);
        when(mockRelativeDuration.start()).thenReturn(Optional.of(Period.parse("P-5D")));
        when(mockRelativeDuration.end()).thenReturn(Optional.of(Period.parse("P10D")));
        DataNeedWrapper wrapper = new DataNeedWrapper(mockDataNeed, LocalDate.of(2024, 4, 1), LocalDate.of(2024, 4, 4));
        when(mockDataNeedsService.findDataNeedAndCalculateStartAndEnd(any(), any(), any(), any())).thenReturn(wrapper);

        var expectedStart = LocalDate.parse("2024-04-01");
        var expectedExpiration = LocalDate.parse("2024-04-04");


        // When
        var dto = service.createValidateAndSendPermissionRequest(new PermissionRequestForCreation(connectionId,
                                                                                                  dataNeedId));

        // Then
        assertDoesNotThrow(() -> UUID.fromString(dto.permissionId()));
        assertEquals(connectionId, dto.connectionId());
        assertEquals(dataNeedId, dto.dataNeedId());
        assertEquals("FooBar", dto.serviceName());
        assertEquals(expectedStart, LocalDate.ofInstant(dto.startTime(), REGION_CONNECTOR_ZONE_ID));
        assertEquals(expectedExpiration, LocalDate.ofInstant(dto.expirationTime(), REGION_CONNECTOR_ZONE_ID));
    }

    @Test
    void givenValidInput_createValidateAndSendPermissionRequest_returnsAsExpected() throws DataNeedNotFoundException, UnsupportedDataNeedException {
        // Given
        var start = LocalDate.parse("2023-01-01");
        var end = LocalDate.parse("2023-01-25");
        when(mockDataNeed.id()).thenReturn(dataNeedId);
        when(mockDataNeed.name()).thenReturn("Test Service");
        when(mockDataNeed.dataTags()).thenReturn(Set.of("1-0:1.8.0", "1-0:1.7.0"));
        DataNeedWrapper wrapper = new DataNeedWrapper(mockDataNeed, start, end);
        when(mockDataNeedsService.findDataNeedAndCalculateStartAndEnd(any(), any(), any(), any())).thenReturn(wrapper);


        String permissionId = "8390505d-2bb1-4321-bad2-396897cad525";
        AiidaPermissionRequest request = new AiidaPermissionRequest(permissionId,
                                                                    connectionId,
                                                                    dataNeedId,
                                                                    start,
                                                                    end,
                                                                    PermissionProcessStatus.CREATED,
                                                                    "terminationTopic",
                                                                    "foo",
                                                                    Instant.now());
        var expectedStart = request.start().atStartOfDay(ZoneOffset.UTC).toInstant();
        var expectedEnd = ZonedDateTime.of(request.end(), LocalTime.MAX.withNano(0), ZoneOffset.UTC).toInstant();

        // When
        var dto = service.createValidateAndSendPermissionRequest(new PermissionRequestForCreation(connectionId,
                                                                                                  dataNeedId));


        // Then
        assertAll(
                () -> assertDoesNotThrow(() -> UUID.fromString(dto.permissionId())),
                () -> assertEquals(expectedStart, dto.startTime()),
                () -> assertEquals(expectedEnd, dto.expirationTime()),
                () -> assertEquals(connectionId, dto.connectionId()),
                () -> assertEquals(dataNeedId, dto.dataNeedId()),
                () -> assertEquals("Test Service", dto.serviceName()),
                () -> assertThat(dto.requestedCodes()).hasSameElementsAs(Set.of("1-0:1.8.0", "1-0:1.7.0")),
                () -> assertNotNull(dto.kafkaStreamingConfig())
        );
    }

    @Test
    void givenValidInput_createValidateAndSendPermissionRequest_commitsThreeEvents() throws DataNeedNotFoundException, UnsupportedDataNeedException {
        // Given
        var forCreation = new PermissionRequestForCreation(connectionId, dataNeedId);
        var start = LocalDate.parse("2023-01-01");
        var end = LocalDate.parse("2023-01-25");
        DataNeedWrapper wrapper = new DataNeedWrapper(mockDataNeed, start, end);
        when(mockDataNeedsService.findDataNeedAndCalculateStartAndEnd(any(), any(), any(), any())).thenReturn(wrapper);

        // When
        service.createValidateAndSendPermissionRequest(forCreation);

        // Then
        verify(mockOutbox).commit(argThat(event -> event.status() == PermissionProcessStatus.CREATED));
        verify(mockOutbox).commit(argThat(event -> event.status() == PermissionProcessStatus.VALIDATED));
        verify(mockOutbox).commit(argThat(event -> event.status() == PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR));
    }

    @Test
    void givenNonExistingPermissionId_rejectPermission_throwsException() {
        // Given
        var permissionId = "nonExistingPermissionId";
        when(mockViewRepository.findById(permissionId)).thenReturn(Optional.empty());

        // When, Then
        assertThrows(PermissionNotFoundException.class, () -> service.rejectPermission(permissionId));
    }

    @Test
    void givenPermissionInInvalidState_rejectPermission_throwsException() {
        // Given
        var permissionId = "invalidStateId";
        when(mockViewRepository.findById(permissionId)).thenReturn(Optional.of(mockRequest));
        when(mockRequest.status()).thenReturn(PermissionProcessStatus.CREATED);

        // When, Then
        assertThrows(PermissionStateTransitionException.class, () -> service.rejectPermission(permissionId));
    }

    @Test
    void givenValidInput_rejectPermission_emitsRejectedStatusEvent() throws PermissionNotFoundException, PermissionStateTransitionException {
        // Given
        var permissionId = "testId";
        when(mockViewRepository.findById(permissionId)).thenReturn(Optional.of(mockRequest));
        when(mockRequest.status()).thenReturn(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR);

        // When
        service.rejectPermission(permissionId);

        // Then
        verify(mockOutbox).commit(argThat(event -> event.status() == PermissionProcessStatus.REJECTED
                && event.permissionId().equals(permissionId)));
    }

    @Test
    void givenNonExistingPermissionId_unableToFulFillPermission_throwsException() {
        // Given
        var permissionId = "nonExistingPermissionId";
        when(mockViewRepository.findById(permissionId)).thenReturn(Optional.empty());

        // When, Then
        assertThrows(PermissionNotFoundException.class, () -> service.unableToFulFillPermission(permissionId));
    }

    @Test
    void givenPermissionInInvalidState_unableToFulFillPermission_throwsException() {
        // Given
        var permissionId = "invalidStateId";
        when(mockViewRepository.findById(permissionId)).thenReturn(Optional.of(mockRequest));
        when(mockRequest.status()).thenReturn(PermissionProcessStatus.CREATED);

        // When, Then
        assertThrows(PermissionStateTransitionException.class, () -> service.unableToFulFillPermission(permissionId));
    }

    @Test
    void givenValidInput_unableToFulFillPermission_emitsUnfulfillableStatusEvent() throws PermissionNotFoundException, PermissionStateTransitionException {
        // Given
        var permissionId = "testId";
        when(mockViewRepository.findById(permissionId)).thenReturn(Optional.of(mockRequest));
        when(mockRequest.status()).thenReturn(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR);

        // When
        service.unableToFulFillPermission(permissionId);

        // Then
        verify(mockOutbox).commit(argThat(event -> event.status() == PermissionProcessStatus.UNFULFILLABLE
                && event.permissionId().equals(permissionId)));
    }

    @Test
    void givenNonExistingPermissionId_acceptPermission_throwsException() {
        // Given
        var permissionId = "nonExistingPermissionId";
        when(mockViewRepository.findById(permissionId)).thenReturn(Optional.empty());

        // When, Then
        assertThrows(PermissionNotFoundException.class, () -> service.acceptPermission(permissionId));
    }

    @Test
    void givenPermissionInInvalidState_acceptPermission_throwsException() {
        // Given
        var permissionId = "permissionId";
        when(mockViewRepository.findById(permissionId)).thenReturn(Optional.of(mockRequest));
        when(mockRequest.status()).thenReturn(PermissionProcessStatus.CREATED);

        // When, Then
        assertThrows(PermissionStateTransitionException.class, () -> service.acceptPermission(permissionId));
    }

    @Test
    void givenValidInput_acceptPermission_createsCredentials_andEmitsAcceptedAndCreatedCredentialsEvent() throws PermissionNotFoundException, PermissionStateTransitionException, CredentialsAlreadyExistException {
        // Given
        var permissionId = "testId";
        when(mockViewRepository.findById(permissionId)).thenReturn(Optional.of(mockRequest));
        when(mockRequest.status()).thenReturn(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR);
        when(mockMqttService.createCredentialsAndAclForPermission(permissionId)).thenReturn(mockMqttDto);

        // When
        MqttDto mqttDto = service.acceptPermission(permissionId);

        // Then
        verify(mockOutbox, times(2)).commit(permissionEventCaptor.capture());
        assertEquals(2, permissionEventCaptor.getAllValues().size());
        assertEquals(PermissionProcessStatus.ACCEPTED, permissionEventCaptor.getAllValues().getFirst().status());
        assertEquals(permissionId, permissionEventCaptor.getAllValues().getFirst().permissionId());

        assertInstanceOf(MqttCredentialsCreatedEvent.class, permissionEventCaptor.getAllValues().get(1));
        assertEquals(PermissionProcessStatus.ACCEPTED, permissionEventCaptor.getAllValues().get(1).status());
        assertEquals(permissionId, permissionEventCaptor.getAllValues().get(1).permissionId());
    }
}
