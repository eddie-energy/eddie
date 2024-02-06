package energy.eddie.regionconnector.dk.energinet.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.dk.energinet.permission.request.EnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.permission.request.PermissionRequestFactory;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetCustomerPermissionRequestRepository;
import energy.eddie.regionconnector.dk.energinet.permission.request.states.EnerginetCustomerAcceptedState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionRequestServiceTest {
    @Mock
    private DkEnerginetCustomerPermissionRequestRepository repository;
    @Mock
    private EnerginetCustomerApi customerApi;
    @Mock
    private PermissionRequestFactory requestFactory;
    private PermissionRequestService service;

    @BeforeEach
    void setUp() {
        service = new PermissionRequestService(repository, requestFactory);
    }

    @Test
    void givenNonExistingId_findConnectionStatusMessageById_returnsEmptyOptional() {
        var permissionId = "NonExistingId";

        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.empty());

        Optional<ConnectionStatusMessage> status = service.findConnectionStatusMessageById(permissionId);
        assertTrue(status.isEmpty());
    }

    @Test
    void givenExistingId_findConnectionStatusMessageById_returnsNonEmptyOptional() {
        // Given
        var permissionId = "a0ec0288-7eaf-4aa2-8387-77c6413cfd31";
        String connectionId = "connId";
        String dataNeedId = "dataNeedId";
        var permissionRequest = mock(EnerginetCustomerPermissionRequest.class);
        var state = new EnerginetCustomerAcceptedState(permissionRequest);

        doReturn(permissionId).when(permissionRequest).permissionId();
        doReturn(connectionId).when(permissionRequest).connectionId();
        doReturn(dataNeedId).when(permissionRequest).dataNeedId();
        doReturn(state).when(permissionRequest).state();

        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.of(permissionRequest));

        // When
        Optional<ConnectionStatusMessage> optionalStatus = service.findConnectionStatusMessageById(permissionId);

        // Then
        assertTrue(optionalStatus.isPresent());
        var status = optionalStatus.get();
        assertEquals(permissionId, status.permissionId());
        assertEquals(connectionId, status.connectionId());
        assertEquals(dataNeedId, status.dataNeedId());
        assertEquals(PermissionProcessStatus.ACCEPTED, status.status());
        assertNotNull(status.timestamp());
    }

    @Test
    void givenNonExistingId_findPermissionRequestById_returnsEmptyOptional() {
        var permissionId = "NonExistingId";

        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.empty());

        Optional<DkEnerginetCustomerPermissionRequest> permissionRequest = service.findByPermissionId(permissionId);
        assertTrue(permissionRequest.isEmpty());
    }

    @Test
    void givenExistingId_findPermissionRequestById_returnsNonEmptyOptional() {
        // Given
        var permissionId = "a0ec0288-7eaf-4aa2-8387-77c6413cfd31";
        String connectionId = "connId";
        String dataNeedId = "dataNeedId";
        var start = ZonedDateTime.now(ZoneOffset.UTC);
        var end = start.plusDays(10);
        var creation = new PermissionRequestForCreation(connectionId, start, end, "token", Granularity.PT15M, "mpid", dataNeedId);
        var permissionRequest = new EnerginetCustomerPermissionRequest(permissionId, creation, customerApi);
        var state = new EnerginetCustomerAcceptedState(permissionRequest);
        permissionRequest.changeState(state);

        when(repository.findByPermissionId(permissionId))
                .thenReturn(Optional.of(permissionRequest));
        when(requestFactory.create(permissionRequest))
                .thenReturn(permissionRequest);

        // When
        var optionalPermissionRequest = service.findByPermissionId(permissionId);

        // Then
        assertTrue(optionalPermissionRequest.isPresent());
        var result = optionalPermissionRequest.get();
        assertEquals(permissionId, result.permissionId());
        assertEquals(connectionId, result.connectionId());
        assertEquals(dataNeedId, result.dataNeedId());
        assertEquals(PermissionProcessStatus.ACCEPTED, result.state().status());
    }

    @Test
    void findAllAccepted_returnsAllAcceptedPermissionRequests() {
        // Given
        String connectionId = "connId";
        String dataNeedId = "dataNeedId";
        var start = ZonedDateTime.now(ZoneOffset.UTC);
        var end = start.plusDays(10);
        var creation = new PermissionRequestForCreation(connectionId, start, end, "token", Granularity.PT15M, "mpid", dataNeedId);
        var permissionRequest1 = new EnerginetCustomerPermissionRequest(UUID.randomUUID().toString(), creation, customerApi);
        permissionRequest1.changeState(new EnerginetCustomerAcceptedState(permissionRequest1));
        var permissionRequest2 = new EnerginetCustomerPermissionRequest(UUID.randomUUID().toString(), creation, customerApi);
        when(requestFactory.create(permissionRequest1))
                .thenReturn(permissionRequest1);

        when(repository.findAll())
                .thenReturn(List.of(permissionRequest1, permissionRequest2));

        // When
        var res = service.findAllAcceptedPermissionRequests();

        // Then
        assertEquals(List.of(permissionRequest1), res);
    }
}