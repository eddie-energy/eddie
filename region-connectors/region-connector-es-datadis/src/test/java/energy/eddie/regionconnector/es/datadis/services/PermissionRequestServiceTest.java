package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.PermissionRequestFactory;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequestRepository;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.Optional;

import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.ZONE_ID_SPAIN;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionRequestServiceTest {
    @Mock
    private EsPermissionRequestRepository repository;
    @Mock
    private PermissionRequestFactory factory;
    @Mock
    private DatadisScheduler scheduler;
    @InjectMocks
    private PermissionRequestService service;

    @Test
    void findConnectionStatusMessageById_nonExistingId_returnsEmptyOptional() {
        // Given
        var permissionId = "nonExisting";
        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.empty());

        // When
        Optional<ConnectionStatusMessage> statusMessage = service.findConnectionStatusMessageById(permissionId);

        // Then
        assertTrue(statusMessage.isEmpty());
    }

    @Test
    void getAllAcceptedPermissionRequests() {
        // Given
        EsPermissionRequest permissionRequest = mock(EsPermissionRequest.class);
        when(factory.create(permissionRequest)).thenReturn(permissionRequest);
        when(repository.findAllAccepted()).thenReturn(Stream.of(permissionRequest));


        // When
        var acceptedPermissionRequests = service.getAllAcceptedPermissionRequests().toList();

        // Then
        assertEquals(1, acceptedPermissionRequests.size());
        assertEquals(permissionRequest, acceptedPermissionRequests.getFirst());
    }

    @Test
    void findConnectionStatusMessageById_existingId_returnsPopulatedStatusMessage() {
        // Given
        var permissionId = "Existing";
        var connectionId = "bar";
        var dataNeedId = "luu";
        var nif = "muh";
        var meteringPointId = "kuh";
        var now = ZonedDateTime.now(ZONE_ID_SPAIN);
        var requestDataFrom = now.minusDays(10);
        var requestDataTo = now.minusDays(5);
        var measurementType = MeasurementType.QUARTER_HOURLY;
        var requestForCreation = new PermissionRequestForCreation(connectionId, dataNeedId, nif, meteringPointId,
                requestDataFrom, requestDataTo, measurementType);
        var permissionRequest = new DatadisPermissionRequest(permissionId, requestForCreation, mock(AuthorizationApi.class));
        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.of(permissionRequest));

        // When
        Optional<ConnectionStatusMessage> optional = service.findConnectionStatusMessageById(permissionId);

        // Then
        assertTrue(optional.isPresent());
        var statusMessage = optional.get();
        assertEquals(permissionId, statusMessage.permissionId());
        assertEquals(connectionId, statusMessage.connectionId());
        assertEquals(dataNeedId, statusMessage.dataNeedId());
        assertEquals(PermissionProcessStatus.CREATED, statusMessage.status());
    }

    @Test
    void acceptPermission_nonExistingId_throws() {
        // Given
        var permissionId = "nonExisting";
        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.empty());

        // When, Then
        assertThrows(PermissionNotFoundException.class, () -> service.acceptPermission(permissionId));
    }

    @Test
    void acceptPermission_existingId_acceptsPermissionRequest_andSchedulesPoll() throws StateTransitionException {
        // Given
        var permissionId = "Existing";
        var permissionRequest = mock(DatadisPermissionRequest.class);
        when(permissionRequest.permissionId()).thenReturn(permissionId);
        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.of(permissionRequest));
        when(factory.create(permissionRequest)).thenReturn(permissionRequest);

        // When
        assertDoesNotThrow(() -> service.acceptPermission(permissionId));

        // Then
        verify(permissionRequest).accept();
        verify(scheduler).pullAvailableHistoricalData(argThat(arg -> arg.permissionId().equals(permissionId)));
        verifyNoMoreInteractions(permissionRequest);
    }

    @Test
    void rejectPermission_nonExistingId_throws() {
        // Given
        var permissionId = "nonExisting";
        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.empty());

        // When, Then
        assertThrows(PermissionNotFoundException.class, () -> service.rejectPermission(permissionId));
    }

    @Test
    void rejectPermission_existingId_callsReject() throws StateTransitionException {
        // Given
        var permissionId = "Existing";
        var permissionRequest = mock(DatadisPermissionRequest.class);
        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.of(permissionRequest));
        when(factory.create(permissionRequest)).thenReturn(permissionRequest);

        // When
        assertDoesNotThrow(() -> service.rejectPermission(permissionId));

        // Then
        verify(permissionRequest).reject();
        verifyNoInteractions(scheduler);
    }

    @Test
    void createAndSendPermissionRequest_callsValidateAndSendToPaAndReceivedPaResponse() throws StateTransitionException {
        // Given
        var mockCreationRequest = mock(PermissionRequestForCreation.class);
        var mockPermissionRequest = mock(EsPermissionRequest.class);
        when(factory.create(any(PermissionRequestForCreation.class))).thenReturn(mockPermissionRequest);

        // When
        service.createAndSendPermissionRequest(mockCreationRequest);

        // Then
        verify(mockPermissionRequest).validate();
        verify(mockPermissionRequest).sendToPermissionAdministrator();
        verify(mockPermissionRequest).receivedPermissionAdministratorResponse();
        verifyNoMoreInteractions(mockPermissionRequest);
    }

    @Test
    void terminatePermission_nonExistingId_throws() {
        // Given
        var permissionId = "nonExisting";
        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.empty());

        // When, Then
        assertThrows(PermissionNotFoundException.class, () -> service.terminatePermission(permissionId));
    }

    @Test
    void terminatePermission_existingId_callsTerminate() throws StateTransitionException {
        // Given
        var permissionId = "Existing";
        var permissionRequest = mock(DatadisPermissionRequest.class);
        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.of(permissionRequest));
        when(factory.create(permissionRequest)).thenReturn(permissionRequest);

        // When
        assertDoesNotThrow(() -> service.terminatePermission(permissionId));

        // Then
        verify(permissionRequest).terminate();
        verifyNoMoreInteractions(permissionRequest);
    }
}