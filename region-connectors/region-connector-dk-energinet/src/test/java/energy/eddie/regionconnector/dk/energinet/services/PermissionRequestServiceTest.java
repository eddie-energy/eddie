package energy.eddie.regionconnector.dk.energinet.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.process.model.PastStateException;
import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.EnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.PermissionRequestFactory;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequestRepository;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.states.EnerginetCustomerAcceptedState;
import energy.eddie.regionconnector.dk.energinet.dtos.PermissionRequestForCreation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnector.DK_ZONE_ID;
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
    @Mock
    private Sinks.Many<ConsumptionRecord> consumptionRecordSink;
    @InjectMocks
    private PermissionRequestService service;

    @Test
    void givenNonExistingId_findConnectionStatusMessageById_returnsEmptyOptional() {
        var permissionId = "NonExistingId";

        when(repository.findByPermissionId(permissionId)).thenReturn(Optional.empty());

        Optional<ConnectionStatusMessage> status = service.findConnectionStatusMessageById(permissionId);
        assertTrue(status.isEmpty());
    }

    @Test
    void givenExistingId_findConnectionStatusMessageById_returnsEmptyOptional() {
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
    void createAndSendPermissionRequest_callsApi() throws StateTransitionException {
        // Given
        var start = ZonedDateTime.now(DK_ZONE_ID).minusDays(10);
        var end = start.plusDays(5);
        String connectionId = "connId";
        String dataNeedId = "dataNeedId";
        String refreshToken = "token";
        String meteringPoint = "meteringPoint";
        PermissionRequestForCreation requestForCreation = new PermissionRequestForCreation(connectionId, start, end,
                refreshToken, Granularity.PT1H, meteringPoint, dataNeedId);

        DkEnerginetCustomerPermissionRequest mockRequest = mock(DkEnerginetCustomerPermissionRequest.class);
        when(mockRequest.permissionId()).thenReturn(UUID.randomUUID().toString());
        when(mockRequest.accessToken()).thenReturn(Mono.just("accessToken"));
        when(requestFactory.create(requestForCreation)).thenReturn(mockRequest);
        when(customerApi.getTimeSeries(any(), any(), any(), any(), anyString(), any()))
                .thenReturn(Mono.just(new ConsumptionRecord()));

        // When
        service.createAndSendPermissionRequest(requestForCreation);


        // Then
        verify(requestFactory).create(requestForCreation);
        verify(mockRequest).validate();
        verify(mockRequest).sendToPermissionAdministrator();
        verify(mockRequest).receivedPermissionAdministratorResponse();
        verify(mockRequest).accessToken();
        verify(customerApi).getTimeSeries(any(), any(), any(), any(), anyString(), any());
    }

    @Test
    void createAndSendPermissionRequest_doesNotCallApiOnDeniedPermissionRequest() throws StateTransitionException {
        // Given
        var start = ZonedDateTime.now(DK_ZONE_ID).minusDays(10);
        var end = start.plusDays(5);
        String connectionId = "connId";
        String dataNeedId = "dataNeedId";
        String refreshToken = "token";
        String meteringPoint = "meteringPoint";
        PermissionRequestForCreation requestForCreation = new PermissionRequestForCreation(connectionId, start, end,
                refreshToken, Granularity.PT1H, meteringPoint, dataNeedId);

        DkEnerginetCustomerPermissionRequest mockRequest = mock(DkEnerginetCustomerPermissionRequest.class);
        when(requestFactory.create(requestForCreation)).thenReturn(mockRequest);
        doThrow(PastStateException.class).when(mockRequest).accept();

        // When
        service.createAndSendPermissionRequest(requestForCreation);

        // Then
        verify(requestFactory).create(requestForCreation);
        verify(mockRequest).validate();
        verify(mockRequest).sendToPermissionAdministrator();
        verify(mockRequest).receivedPermissionAdministratorResponse();
        verify(mockRequest, never()).accessToken();
        verify(customerApi, never()).getTimeSeries(any(), any(), any(), any(), anyString(), any());
    }

    @Test
    void close_emitsCompleteOnPublisher() {
        when(consumptionRecordSink.asFlux()).thenReturn(Flux.empty());

        // Given
        StepVerifier.create(JdkFlowAdapter.flowPublisherToFlux(service.getConsumptionRecordStream()))
                // When
                .then(service::close)
                // Then
                .expectComplete()
                .verify(Duration.ofSeconds(2));
    }
}