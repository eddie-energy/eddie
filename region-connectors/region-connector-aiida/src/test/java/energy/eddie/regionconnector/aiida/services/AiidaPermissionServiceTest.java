package energy.eddie.regionconnector.aiida.services;

import energy.eddie.api.agnostic.aiida.mqtt.MqttDto;
import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.api.agnostic.process.model.PermissionStateTransitionException;
import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.aiida.AiidaDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.aiida.config.AiidaConfiguration;
import energy.eddie.regionconnector.aiida.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.aiida.exceptions.CredentialsAlreadyExistException;
import energy.eddie.regionconnector.aiida.mqtt.MqttService;
import energy.eddie.regionconnector.aiida.permission.request.AiidaPermissionRequest;
import energy.eddie.regionconnector.aiida.permission.request.events.AiidaIdReceivedEvent;
import energy.eddie.regionconnector.aiida.permission.request.events.FailedToTerminateEvent;
import energy.eddie.regionconnector.aiida.permission.request.events.MqttCredentialsCreatedEvent;
import energy.eddie.regionconnector.aiida.permission.request.persistence.AiidaPermissionRequestViewRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import nl.altindag.log.LogCaptor;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import reactor.core.publisher.Sinks;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static energy.eddie.api.v0.PermissionProcessStatus.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiidaPermissionServiceTest {
    private static final String HANDSHAKE_URL = "http://localhost:8080/region-connectors/aiida/permission-request/{permissionId}";
    private final String connectionId = "testConnId";
    private final String dataNeedId = "testDataNeedId";
    private final String permissionId = "10000000-0000-0000-0000-000000000000";
    private final UUID aiidaId = UUID.fromString("00000000-0000-0000-0000-000000000000");
    @Spy
    @SuppressWarnings("unused")
    private final AiidaConfiguration config = new AiidaConfiguration("customerId",
                                                                     4,
                                                                     HANDSHAKE_URL,
                                                                     "tcp://localhost:1883",
                                                                     null);
    private final LogCaptor logCaptor = LogCaptor.forClass(AiidaPermissionService.class);
    @Mock
    private DataNeedsService mockDataNeedsService;
    @Mock
    private Outbox mockOutbox;
    @Mock
    private AiidaDataNeed mockDataNeed;
    @Mock
    private AiidaPermissionRequestViewRepository mockViewRepository;
    @Mock
    private MqttService mockMqttService;
    @Mock
    private AiidaPermissionRequest mockRequest;
    @Mock
    private MqttDto mockMqttDto;
    @Mock
    private DataNeedCalculationService<DataNeed> calculationService;
    @Mock
    private ApplicationContext applicationContext;
    private AiidaPermissionService service;
    @Captor
    private ArgumentCaptor<PermissionEvent> permissionEventCaptor;

    @BeforeEach
    void setUp() {
        service = new AiidaPermissionService(mockOutbox,
                                             mockDataNeedsService,
                                             config,
                                             mockMqttService,
                                             mockViewRepository,
                                             calculationService,
                                             Sinks.many().multicast().onBackpressureBuffer(),
                                             applicationContext);
    }

    @AfterEach
    void tearDown() {
        logCaptor.clearLogs();
    }

    @Test
    void givenContextRefreshedEvent_subscribesToActivePermissions() throws MqttException {
        // given
        var permission1 = mock(AiidaPermissionRequest.class);
        var permission2 = mock(AiidaPermissionRequest.class);
        when(permission1.permissionId()).thenReturn("perm-1");
        when(permission2.permissionId()).thenReturn("perm-2");
        when(mockViewRepository.findActivePermissionRequests()).thenReturn(List.of(permission1, permission2));

        // when
        service.onApplicationEvent(mock(ContextRefreshedEvent.class));

        // then
        verify(mockViewRepository).findActivePermissionRequests();
        verify(mockMqttService).subscribeToOutboundDataTopic("perm-1");
        verify(mockMqttService).subscribeToStatusTopic("perm-1");
        verify(mockMqttService).subscribeToOutboundDataTopic("perm-2");
        verify(mockMqttService).subscribeToStatusTopic("perm-2");
    }

    @Test
    void givenNonExistingDataNeedId_createValidateAndSendPermissionRequest_throwsException() {
        // Given
        var nonExisting = "NonExisting";
        when(calculationService.calculate(anyString())).thenReturn(new DataNeedNotFoundResult());

        // Then
        assertThrows(DataNeedNotFoundException.class,
                     // When
                     () -> service.createValidateAndSendPermissionRequest(new PermissionRequestForCreation("testConnId",
                                                                                                           nonExisting)));
    }

    @Test
    void givenUnsupportedDataNeed_createValidateAndSendPermissionRequest_throwsException() {
        // Given
        when(calculationService.calculate(anyString())).thenReturn(new DataNeedNotSupportedResult(""));

        // When, Then
        assertThrows(UnsupportedDataNeedException.class,
                     () -> service.createValidateAndSendPermissionRequest(new PermissionRequestForCreation(connectionId,
                                                                                                           dataNeedId)));
    }

    @Test
    void givenValidInput_createValidateAndSendPermissionRequest_returnsAsExpected() throws DataNeedNotFoundException, UnsupportedDataNeedException {
        // Given
        var start = LocalDate.now(ZoneOffset.UTC);
        var end = start.plusDays(24);
        when(mockDataNeed.name()).thenReturn("Test Service");
        when(mockDataNeedsService.getById(anyString())).thenReturn(mockDataNeed);
        when(calculationService.calculate(anyString())).thenReturn(new ValidatedHistoricalDataDataNeedResult(List.of(),
                                                                                                             new Timeframe(
                                                                                                                     start,
                                                                                                                     end),
                                                                                                             new Timeframe(
                                                                                                                     start,
                                                                                                                     end)));

        // When
        var dto = service.createValidateAndSendPermissionRequest(new PermissionRequestForCreation(connectionId,
                                                                                                  dataNeedId));

        // Then
        var expectedHandshakeUrl = HANDSHAKE_URL.substring(0,
                                                           HANDSHAKE_URL.indexOf("{permissionId}")) + dto.permissionId();
        assertAll(() -> assertDoesNotThrow(dto::permissionId),
                  () -> assertEquals("Test Service", dto.serviceName()),
                  () -> assertEquals(expectedHandshakeUrl, dto.handshakeUrl()));
    }

    @Test
    void givenValidInput_createValidateAndSendPermissionRequest_commitsThreeEvents() throws DataNeedNotFoundException, UnsupportedDataNeedException {
        // Given
        var forCreation = new PermissionRequestForCreation(connectionId, dataNeedId);
        var start = LocalDate.now(ZoneOffset.UTC);
        var end = start.plusDays(24);
        when(mockDataNeedsService.getById(anyString())).thenReturn(mockDataNeed);
        when(calculationService.calculate(anyString())).thenReturn(new ValidatedHistoricalDataDataNeedResult(List.of(),
                                                                                                             new Timeframe(
                                                                                                                     start,
                                                                                                                     end),
                                                                                                             new Timeframe(
                                                                                                                     start,
                                                                                                                     end)));

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
        when(mockViewRepository.findById(permissionId)).thenReturn(Optional.empty());

        // When, Then
        assertThrows(PermissionNotFoundException.class, () -> service.rejectPermission(permissionId, aiidaId));
    }

    @Test
    void givenPermissionInInvalidState_rejectPermission_throwsException() {
        // Given
        when(mockViewRepository.findById(permissionId)).thenReturn(Optional.of(mockRequest));
        when(mockRequest.status()).thenReturn(PermissionProcessStatus.CREATED);

        // When, Then
        assertThrows(PermissionStateTransitionException.class, () -> service.rejectPermission(permissionId, aiidaId));
    }

    @Test
    void givenValidInput_rejectPermission_emitsRejectedStatusEvent() throws PermissionNotFoundException, PermissionStateTransitionException, NoSuchFieldException, IllegalAccessException {
        // Given
        when(mockViewRepository.findById(permissionId)).thenReturn(Optional.of(mockRequest));
        when(mockRequest.status()).thenReturn(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR);

        // When
        service.rejectPermission(permissionId, aiidaId);

        // Then
        verify(mockOutbox, times(1)).commit(permissionEventCaptor.capture());
        var aiidaIdReceivedEvent = (AiidaIdReceivedEvent) permissionEventCaptor.getAllValues().getFirst();
        testAiidaIdReceivedEvent(aiidaIdReceivedEvent, REJECTED);

        var actualAiidaId = aiidaIdThroughReflection(aiidaIdReceivedEvent);
        assertEquals(aiidaId, actualAiidaId);
    }

    @Test
    void givenNonExistingPermissionId_unableToFulfillPermission_throwsException() {
        // Given
        when(mockViewRepository.findById(permissionId)).thenReturn(Optional.empty());

        // When, Then
        assertThrows(PermissionNotFoundException.class, () -> service.unableToFulfillPermission(permissionId, aiidaId));
    }

    @Test
    void givenPermissionInInvalidState_unableToFulfillPermission_throwsException() {
        // Given
        when(mockViewRepository.findById(permissionId)).thenReturn(Optional.of(mockRequest));
        when(mockRequest.status()).thenReturn(PermissionProcessStatus.CREATED);

        // When, Then
        assertThrows(PermissionStateTransitionException.class,
                     () -> service.unableToFulfillPermission(permissionId, aiidaId));
    }

    @Test
    void givenValidInput_unableToFulfillPermission_emitsUnfulfillableStatusEvent() throws PermissionNotFoundException, PermissionStateTransitionException, NoSuchFieldException, IllegalAccessException {
        // Given
        when(mockViewRepository.findById(permissionId)).thenReturn(Optional.of(mockRequest));
        when(mockRequest.status()).thenReturn(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR);

        // When
        service.unableToFulfillPermission(permissionId, aiidaId);

        // Then
        verify(mockOutbox, times(1)).commit(permissionEventCaptor.capture());
        var aiidaIdReceivedEvent = (AiidaIdReceivedEvent) permissionEventCaptor.getAllValues().getFirst();
        testAiidaIdReceivedEvent(aiidaIdReceivedEvent, UNFULFILLABLE);

        var actualAiidaId = aiidaIdThroughReflection(aiidaIdReceivedEvent);
        assertEquals(aiidaId, actualAiidaId);
    }

    @Test
    void givenNonExistingPermissionId_revokePermission_throwsException() {
        // Given
        when(mockViewRepository.findById(permissionId)).thenReturn(Optional.empty());

        // When
        service.revokePermission(permissionId);

        // Then
        assertTrue(logCaptor.getErrorLogs().contains("Permission with permission id " + permissionId + " not found"));
    }

    @Test
    void givenPermissionInInvalidState_revokePermission_throwsException() {
        // Given
        when(mockViewRepository.findById(permissionId)).thenReturn(Optional.of(mockRequest));
        when(mockRequest.status()).thenReturn(PermissionProcessStatus.CREATED);

        // When
        service.revokePermission(permissionId);

        // Then
        assertTrue(logCaptor.getErrorLogs()
                            .contains("Permission with permission id " + permissionId + " could not be revoked"));
    }

    @Test
    void givenValidInput_revokePermission_deletesAclsAndEmitsRevokedStatusEvent() {
        // Given
        when(mockViewRepository.findById(permissionId)).thenReturn(Optional.of(mockRequest));
        when(mockRequest.status()).thenReturn(ACCEPTED);

        // When
        service.revokePermission(permissionId);

        // Then
        verify(mockMqttService).deleteAclsForPermission(permissionId);
        verify(mockOutbox).commit(argThat(event -> event.status() == PermissionProcessStatus.REVOKED && event.permissionId()
                                                                                                             .equals(permissionId)));
    }

    @Test
    void givenPermissionInInvalidState_externallyTerminatePermission_throwsException() {
        // Given
        when(mockViewRepository.findById(permissionId)).thenReturn(Optional.of(mockRequest));
        when(mockRequest.status()).thenReturn(TERMINATED);

        // When
        service.externallyTerminatePermission(permissionId);

        // Then
        verify(mockMqttService, never()).deleteAclsForPermission(permissionId);
        verify(mockOutbox, never()).commit(any());
    }

    @Test
    void givenValidInput_externallyTerminatePermission_deletesAclsAndEmitsTerminatedStatusEvent() {
        // Given
        when(mockViewRepository.findById(permissionId)).thenReturn(Optional.of(mockRequest));
        when(mockRequest.status()).thenReturn(REQUIRES_EXTERNAL_TERMINATION);

        // When
        service.externallyTerminatePermission(permissionId);

        // Then
        verify(mockMqttService).deleteAclsForPermission(permissionId);
        verify(mockOutbox).commit(argThat(event -> event.status() == EXTERNALLY_TERMINATED && event.permissionId()
                                                                                                   .equals(permissionId)));
    }

    @Test
    void givenPermissionInInvalidState_fulfillPermission_throwsException() {
        // Given
        when(mockViewRepository.findById(permissionId)).thenReturn(Optional.of(mockRequest));
        when(mockRequest.status()).thenReturn(PermissionProcessStatus.CREATED);

        // When
        service.fulfillPermission(permissionId);

        // Then
        verify(mockMqttService, never()).deleteAclsForPermission(permissionId);
        verify(mockOutbox, never()).commit(any());
    }

    @Test
    void givenValidInput_fulfillPermission_deletesAclsAndEmitsFulfilledStatusEvent() {
        // Given
        when(mockViewRepository.findById(permissionId)).thenReturn(Optional.of(mockRequest));
        when(mockRequest.status()).thenReturn(ACCEPTED);

        // When
        service.fulfillPermission(permissionId);

        // Then
        verify(mockMqttService).deleteAclsForPermission(permissionId);
        verify(mockOutbox).commit(argThat(event -> event.status() == FULFILLED && event.permissionId()
                                                                                       .equals(permissionId)));
    }

    @Test
    void givenNonExistingPermissionId_acceptPermission_throwsException() {
        // Given
        when(mockViewRepository.findById(permissionId)).thenReturn(Optional.empty());

        // When, Then
        assertThrows(PermissionNotFoundException.class, () -> service.acceptPermission(permissionId, aiidaId));
    }

    @Test
    void givenPermissionInInvalidState_acceptPermission_throwsException() {
        // Given
        when(mockViewRepository.findById(permissionId)).thenReturn(Optional.of(mockRequest));
        when(mockRequest.status()).thenReturn(PermissionProcessStatus.CREATED);

        // When, Then
        assertThrows(PermissionStateTransitionException.class, () -> service.acceptPermission(permissionId, aiidaId));
    }

    @Test
    void givenValidInput_acceptPermission_subscribeToStatusTopic_throwsException() throws CredentialsAlreadyExistException, PermissionNotFoundException, PermissionStateTransitionException, MqttException, DataNeedNotFoundException {
        // Given
        when(mockViewRepository.findById(permissionId)).thenReturn(Optional.of(mockRequest));
        when(mockViewRepository.findByPermissionId(permissionId)).thenReturn(Optional.of(mockRequest));
        when(mockRequest.dataNeedId()).thenReturn(dataNeedId);
        when(mockDataNeedsService.findById(dataNeedId)).thenReturn(Optional.of(mockDataNeed));
        when(mockRequest.status()).thenReturn(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR);
        when(mockMqttService.createCredentialsAndAclForPermission(permissionId, false)).thenReturn(mockMqttDto);
        doThrow(MqttException.class).when(mockMqttService).subscribeToStatusTopic(permissionId);

        // When
        service.acceptPermission(permissionId, aiidaId);

        // Then
        assertTrue(logCaptor.getErrorLogs()
                            .contains("Something went wrong when subscribing to a topic for permission " + permissionId));
    }

    @Test
    void givenValidInput_acceptPermission_createsCredentials_andEmitsAcceptedAndCreatedCredentialsEvent() throws PermissionNotFoundException, PermissionStateTransitionException, CredentialsAlreadyExistException, MqttException, NoSuchFieldException, IllegalAccessException, DataNeedNotFoundException {
        // Given
        when(mockViewRepository.findById(permissionId)).thenReturn(Optional.of(mockRequest));
        when(mockViewRepository.findByPermissionId(permissionId)).thenReturn(Optional.of(mockRequest));
        when(mockRequest.dataNeedId()).thenReturn(dataNeedId);
        when(mockDataNeedsService.findById(dataNeedId)).thenReturn(Optional.of(mockDataNeed));
        when(mockRequest.status()).thenReturn(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR);
        when(mockMqttService.createCredentialsAndAclForPermission(permissionId, false)).thenReturn(mockMqttDto);

        // When
        service.acceptPermission(permissionId, aiidaId);

        // Then
        verify(mockOutbox, times(2)).commit(permissionEventCaptor.capture());
        assertEquals(2, permissionEventCaptor.getAllValues().size());
        assertEquals(ACCEPTED, permissionEventCaptor.getAllValues().getFirst().status());
        assertEquals(permissionId, permissionEventCaptor.getAllValues().getFirst().permissionId());

        var aiidaIdReceivedEvent = (AiidaIdReceivedEvent) permissionEventCaptor.getAllValues().getFirst();
        testAiidaIdReceivedEvent(aiidaIdReceivedEvent, ACCEPTED);

        var actualAiidaId = aiidaIdThroughReflection(aiidaIdReceivedEvent);
        assertEquals(aiidaId, actualAiidaId);


        assertInstanceOf(MqttCredentialsCreatedEvent.class, permissionEventCaptor.getAllValues().get(1));
        assertEquals(ACCEPTED, permissionEventCaptor.getAllValues().get(1).status());
        assertEquals(permissionId, permissionEventCaptor.getAllValues().get(1).permissionId());

        verify(mockMqttService).subscribeToOutboundDataTopic(permissionId);
        verify(mockMqttService).subscribeToStatusTopic(permissionId);
    }

    @Test
    void givenPermissionInInvalidState_terminatePermission_emitsFailedToTerminateEvent() {
        // Given
        var expectedMessage = "Cannot transition permission '%s' to state '%s', as it is not in a one of the permitted states '%s' but in state '%s'".formatted(
                permissionId,
                PermissionProcessStatus.TERMINATED.name(),
                List.of(ACCEPTED),
                PermissionProcessStatus.FULFILLED);
        when(mockViewRepository.findById(permissionId)).thenReturn(Optional.of(mockRequest));
        when(mockRequest.status()).thenReturn(PermissionProcessStatus.FULFILLED);

        // When
        service.terminatePermission(permissionId);

        // Then
        verify(mockOutbox).commit(argThat(event -> event.status() == PermissionProcessStatus.FAILED_TO_TERMINATE
                                                   && event instanceof FailedToTerminateEvent failedEvent
                                                   && failedEvent.message()
                                                                 .equals(expectedMessage)));
    }

    @Test
    void givenExceptionDuringTerminate_terminatePermission_emitsFailedToTerminateEvent() throws MqttException {
        // Given
        doThrow(new MqttException(1234567)).when(mockMqttService).sendTerminationRequest(any());
        when(mockViewRepository.findById(permissionId)).thenReturn(Optional.of(mockRequest));
        when(mockRequest.status()).thenReturn(ACCEPTED);

        // When
        service.terminatePermission(permissionId);

        // Then
        verify(mockOutbox).commit(argThat(event -> event.status() == PermissionProcessStatus.FAILED_TO_TERMINATE
                                                   && event instanceof FailedToTerminateEvent failedEvent
                                                   && failedEvent.message().contains("1234567")));
    }

    @Test
    void givenSuccessfulSend_terminatePermission_emitsTerminatedEvents() {
        // Given
        when(mockViewRepository.findById(permissionId)).thenReturn(Optional.of(mockRequest));
        when(mockRequest.status()).thenReturn(ACCEPTED);

        // When
        service.terminatePermission(permissionId);

        // Then
        verify(mockOutbox).commit(argThat(event -> event.status() == PermissionProcessStatus.TERMINATED));
        verify(mockOutbox).commit(argThat(event -> event.status() == REQUIRES_EXTERNAL_TERMINATION));
    }

    @Test
    void givenNonExistingPermissionId_detailsForPermission_throws() {
        // Given
        when(mockViewRepository.findByPermissionId(anyString())).thenReturn(Optional.empty());

        // When, Then
        assertThrows(PermissionNotFoundException.class, () -> service.detailsForPermission("nonExistingPermissionId"));
    }

    @Test
    void givenValidPermissionId_detailsForPermission_returnsAsExpected() throws DataNeedNotFoundException, PermissionNotFoundException {
        // Given
        when(mockViewRepository.findByPermissionId(permissionId)).thenReturn(Optional.of(mockRequest));
        when(mockRequest.permissionId()).thenReturn(permissionId);
        when(mockRequest.dataNeedId()).thenReturn("dataNeedId");
        when(mockDataNeedsService.findById("dataNeedId")).thenReturn(Optional.of(mockDataNeed));
        when(mockDataNeed.name()).thenReturn("MyName");

        // When
        var details = service.detailsForPermission(permissionId);

        // Then
        assertEquals(permissionId, details.request().permissionId());
        assertEquals("dataNeedId", details.request().dataNeedId());
        assertEquals("MyName", details.dataNeed().name());
    }

    private void testAiidaIdReceivedEvent(AiidaIdReceivedEvent aiidaIdReceivedEvent, PermissionProcessStatus status) {
        assertInstanceOf(AiidaIdReceivedEvent.class, aiidaIdReceivedEvent);
        assertEquals(status, aiidaIdReceivedEvent.status());
        assertEquals(permissionId, aiidaIdReceivedEvent.permissionId());
    }

    private UUID aiidaIdThroughReflection(AiidaIdReceivedEvent aiidaIdReceivedEvent) throws NoSuchFieldException, IllegalAccessException {
        var aiidaIdField = aiidaIdReceivedEvent.getClass().getDeclaredField("aiidaId");
        aiidaIdField.setAccessible(true);

        return (UUID) aiidaIdField.get(aiidaIdReceivedEvent);
    }
}
