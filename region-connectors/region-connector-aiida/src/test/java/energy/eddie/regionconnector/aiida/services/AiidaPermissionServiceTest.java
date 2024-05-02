package energy.eddie.regionconnector.aiida.services;

import energy.eddie.api.agnostic.process.model.PermissionStateTransitionException;
import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
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
import energy.eddie.regionconnector.shared.exceptions.JwtCreationFailedException;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import energy.eddie.regionconnector.shared.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static energy.eddie.regionconnector.aiida.AiidaRegionConnectorMetadata.REGION_CONNECTOR_ZONE_ID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiidaPermissionServiceTest {
    private final String HANDSHAKE_URL = "http://localhost:8080/region-connectors/aiida/permission-request/{permissionId}";
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
    private AiidaPermissionRequestViewRepository mockViewRepository;
    @Mock
    private MqttService mockMqttService;
    @Mock
    private AiidaPermissionRequest mockRequest;
    @Mock
    private MqttDto mockMqttDto;
    @Mock
    private JwtUtil mockJwtUtil;
    @Captor
    private ArgumentCaptor<PermissionEvent> permissionEventCaptor;
    private AiidaPermissionService service;

    @BeforeEach
    void setUp() {
        PlainAiidaConfiguration config = new PlainAiidaConfiguration(
                "customerId",
                4,
                HANDSHAKE_URL
        );
        var fixedClock = Clock.fixed(Instant.parse("2023-10-15T15:00:00Z"),
                                     REGION_CONNECTOR_ZONE_ID);
        service = new AiidaPermissionService(mockOutbox,
                                             mockDataNeedsService,
                                             fixedClock,
                                             config,
                                             mockMqttService,
                                             mockViewRepository,
                                             mockJwtUtil);
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
    void givenValidInput_createValidateAndSendPermissionRequest_returnsAsExpected() throws DataNeedNotFoundException, UnsupportedDataNeedException, JwtCreationFailedException {
        // Given
        var start = LocalDate.parse("2023-01-01");
        var end = LocalDate.parse("2023-01-25");
        when(mockDataNeed.name()).thenReturn("Test Service");
        DataNeedWrapper wrapper = new DataNeedWrapper(mockDataNeed, start, end);
        when(mockDataNeedsService.findDataNeedAndCalculateStartAndEnd(any(), any(), any(), any())).thenReturn(wrapper);
        when(mockJwtUtil.createAiidaJwt(anyString())).thenReturn("myToken");

        // When
        var dto = service.createValidateAndSendPermissionRequest(new PermissionRequestForCreation(connectionId,
                                                                                                  dataNeedId));

        // Then
        var expectedHandshakeUrl = HANDSHAKE_URL.substring(0,
                                                           HANDSHAKE_URL.indexOf("{permissionId}")) + dto.permissionId();
        assertAll(
                () -> assertDoesNotThrow(() -> UUID.fromString(dto.permissionId())),
                () -> assertEquals("myToken", dto.accessToken()),
                () -> assertEquals("Test Service", dto.serviceName()),
                () -> assertEquals(expectedHandshakeUrl, dto.handshakeUrl())
        );
    }

    @Test
    void givenValidInput_createValidateAndSendPermissionRequest_commitsThreeEvents() throws DataNeedNotFoundException, UnsupportedDataNeedException, JwtCreationFailedException {
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
        service.acceptPermission(permissionId);

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
