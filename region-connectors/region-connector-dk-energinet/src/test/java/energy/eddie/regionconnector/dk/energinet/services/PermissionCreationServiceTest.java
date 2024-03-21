package energy.eddie.regionconnector.dk.energinet.services;

import energy.eddie.api.agnostic.process.model.PastStateException;
import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.regionconnector.dk.energinet.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.dk.energinet.permission.request.PermissionRequestFactory;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetCustomerPermissionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionCreationServiceTest {
    @Mock
    private PermissionRequestFactory requestFactory;
    @Mock
    private PollingService pollingService;
    private PermissionCreationService service;

    @BeforeEach
    void setUp() {
        service = new PermissionCreationService(requestFactory, pollingService);
    }

    @Test
    void createAndSendPermissionRequest_doesNotCallApiOnDeniedPermissionRequest() throws StateTransitionException, DataNeedNotFoundException, UnsupportedDataNeedException {
        // Given
        String connectionId = "connId";
        String dataNeedId = "dataNeedId";
        String refreshToken = "token";
        String meteringPoint = "meteringPoint";
        PermissionRequestForCreation requestForCreation = new PermissionRequestForCreation(connectionId,
                                                                                           refreshToken,
                                                                                           meteringPoint,
                                                                                           dataNeedId);

        DkEnerginetCustomerPermissionRequest mockRequest = mock(DkEnerginetCustomerPermissionRequest.class);
        when(requestFactory.create(requestForCreation)).thenReturn(mockRequest);
        doThrow(PastStateException.class).when(mockRequest).accept();

        // When
        // Then
        assertThrows(PastStateException.class, () -> service.createAndSendPermissionRequest(requestForCreation));
        verify(requestFactory).create(requestForCreation);
        verify(mockRequest).validate();
        verify(mockRequest).sendToPermissionAdministrator();
        verify(mockRequest).receivedPermissionAdministratorResponse();
        verify(mockRequest, never()).accessToken();
    }

    @Test
    void createAndSendPermissionRequest_callsApiOnAcceptedPermissionRequest() throws StateTransitionException, DataNeedNotFoundException, UnsupportedDataNeedException {
        // Given
        String connectionId = "connId";
        String dataNeedId = "dataNeedId";
        String refreshToken = "token";
        String meteringPoint = "meteringPoint";
        PermissionRequestForCreation requestForCreation = new PermissionRequestForCreation(connectionId,
                                                                                           refreshToken,
                                                                                           meteringPoint,
                                                                                           dataNeedId);

        DkEnerginetCustomerPermissionRequest mockRequest = mock(DkEnerginetCustomerPermissionRequest.class);
        when(requestFactory.create(requestForCreation)).thenReturn(mockRequest);

        // When
        service.createAndSendPermissionRequest(requestForCreation);

        // Then
        verify(requestFactory).create(requestForCreation);
        verify(mockRequest).validate();
        verify(mockRequest).sendToPermissionAdministrator();
        verify(mockRequest).receivedPermissionAdministratorResponse();
        verify(pollingService).fetchHistoricalMeterReadings(any());
    }
}
