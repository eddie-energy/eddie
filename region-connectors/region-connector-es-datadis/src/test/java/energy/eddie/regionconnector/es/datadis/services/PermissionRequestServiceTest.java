package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.api.DatadisApiException;
import energy.eddie.regionconnector.es.datadis.consumer.PermissionRequestConsumer;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.es.datadis.dtos.Supply;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.PermissionRequestFactory;
import energy.eddie.regionconnector.es.datadis.permission.request.StateBuilderFactory;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequestRepository;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

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
    private SupplyApiService supplyApiService;
    @Mock
    private PermissionRequestConsumer permissionRequestConsumer;
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
        var now = LocalDate.now(ZONE_ID_SPAIN);
        var requestDataFrom = now.minusDays(10);
        var requestDataTo = now.minusDays(5);
        var requestForCreation = new PermissionRequestForCreation(connectionId, dataNeedId, nif, meteringPointId,
                requestDataFrom, requestDataTo, Granularity.PT15M);
        var permissionRequest = new DatadisPermissionRequest(permissionId, requestForCreation, new StateBuilderFactory(mock(AuthorizationApi.class)));
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
    void acceptPermission_existingId_callsPermissionRequestConsumer() {
        // Given
        var permissionId = "Existing";
        var permissionRequest = mock(DatadisPermissionRequest.class);
        Supply supply = new Supply("", "", "", "", "", "1", LocalDate.now(ZONE_ID_SPAIN), null, 1, "1");
        when(permissionRequest.permissionId()).thenReturn(permissionId);
        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.of(permissionRequest));
        when(factory.create(permissionRequest)).thenReturn(permissionRequest);
        when(supplyApiService.fetchSupplyForPermissionRequest(permissionRequest)).thenReturn(Mono.just(supply));

        // When
        assertDoesNotThrow(() -> service.acceptPermission(permissionId));

        // Then
        verify(permissionRequestConsumer).acceptPermission(permissionRequest, supply);
    }

    @Test
    void acceptPermission_existingId_whenSupplyApiServiceReturnsException_callsErrorConsumer() {
        // Given
        var permissionId = "Existing";
        var permissionRequest = mock(DatadisPermissionRequest.class);
        when(permissionRequest.permissionId()).thenReturn(permissionId);
        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.of(permissionRequest));
        when(factory.create(permissionRequest)).thenReturn(permissionRequest);
        when(supplyApiService.fetchSupplyForPermissionRequest(permissionRequest)).thenReturn(Mono.error(new DatadisApiException("", HttpResponseStatus.FORBIDDEN, "")));

        // When
        assertDoesNotThrow(() -> service.acceptPermission(permissionId));

        // Then
        verify(permissionRequestConsumer).consumeError(any(), eq(permissionRequest));
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
        verifyNoInteractions(permissionRequestConsumer);
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
