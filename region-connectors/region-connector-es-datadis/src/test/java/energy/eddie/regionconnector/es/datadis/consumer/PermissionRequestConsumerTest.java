package energy.eddie.regionconnector.es.datadis.consumer;

import energy.eddie.api.agnostic.process.model.PastStateException;
import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.regionconnector.es.datadis.api.DatadisApiException;
import energy.eddie.regionconnector.es.datadis.dtos.Supply;
import energy.eddie.regionconnector.es.datadis.permission.request.DistributorCode;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.state.AcceptedState;
import energy.eddie.regionconnector.es.datadis.services.HistoricalDataService;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionRequestConsumerTest {

    @Test
    void acceptPermission_setsDistributorCodeAndPointType_AcceptsRequest_FetchesHistoricalData() throws StateTransitionException {
        // Arrange
        HistoricalDataService historicalDataService = mock(HistoricalDataService.class);
        EsPermissionRequest permissionRequest = mock(EsPermissionRequest.class);
        Supply supply = mock(Supply.class);
        when(supply.pointType()).thenReturn(1);
        when(supply.distributorCode()).thenReturn("1");

        PermissionRequestConsumer permissionRequestConsumer = new PermissionRequestConsumer(historicalDataService);

        // Act
        permissionRequestConsumer.acceptPermission(permissionRequest, supply);

        // Assert
        verify(permissionRequest).setDistributorCodeAndPointType(DistributorCode.fromCode(supply.distributorCode()), supply.pointType());
        verify(permissionRequest).accept();
        verify(historicalDataService).fetchAvailableHistoricalData(permissionRequest);
    }

    @Test
    void acceptPermission_acceptThrows_doesNothing() throws StateTransitionException {
        // Arrange
        HistoricalDataService historicalDataService = mock(HistoricalDataService.class);
        EsPermissionRequest permissionRequest = mock(EsPermissionRequest.class);
        doThrow(new PastStateException(AcceptedState.class)).when(permissionRequest).accept();

        Supply supply = mock(Supply.class);
        when(supply.pointType()).thenReturn(1);
        when(supply.distributorCode()).thenReturn("1");

        PermissionRequestConsumer permissionRequestConsumer = new PermissionRequestConsumer(historicalDataService);

        // Act
        permissionRequestConsumer.acceptPermission(permissionRequest, supply);

        // Assert
        verifyNoInteractions(historicalDataService);
    }


    @Test
    void consumeError_ifForbidden_callsTimeOut() throws StateTransitionException {
        // Arrange
        HistoricalDataService historicalDataService = mock(HistoricalDataService.class);
        EsPermissionRequest permissionRequest = mock(EsPermissionRequest.class);
        PermissionRequestConsumer permissionRequestConsumer = new PermissionRequestConsumer(historicalDataService);
        DatadisApiException exception = new DatadisApiException("", HttpResponseStatus.FORBIDDEN, "");

        // Act
        permissionRequestConsumer.consumeError(exception, permissionRequest);

        // Assert
        verify(permissionRequest).timeOut();
        verifyNoInteractions(historicalDataService);
    }

    @Test
    void consumeError_ifInternalServerError_callsInvalid() throws StateTransitionException {
        // Arrange
        HistoricalDataService historicalDataService = mock(HistoricalDataService.class);
        EsPermissionRequest permissionRequest = mock(EsPermissionRequest.class);
        PermissionRequestConsumer permissionRequestConsumer = new PermissionRequestConsumer(historicalDataService);
        DatadisApiException exception = new DatadisApiException("", HttpResponseStatus.INTERNAL_SERVER_ERROR, "");

        // Act
        permissionRequestConsumer.consumeError(exception, permissionRequest);

        // Assert
        verify(permissionRequest).invalid();
        verifyNoInteractions(historicalDataService);
    }

    @Test
    void consumeError_ifRuntimeException_callsInvalid() throws StateTransitionException {
        // Arrange
        HistoricalDataService historicalDataService = mock(HistoricalDataService.class);
        EsPermissionRequest permissionRequest = mock(EsPermissionRequest.class);
        PermissionRequestConsumer permissionRequestConsumer = new PermissionRequestConsumer(historicalDataService);
        RuntimeException exception = new RuntimeException(new RuntimeException("error"));

        // Act
        permissionRequestConsumer.consumeError(exception, permissionRequest);

        // Assert
        verify(permissionRequest).invalid();
        verifyNoInteractions(historicalDataService);
    }
}