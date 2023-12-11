package energy.eddie.regionconnector.es.datadis.permission.request.state;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationResponseHandler;
import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequest;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequestResponse;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class})
class ValidatedStateTest {
    @Mock
    private AuthorizationApi authorizationApi;
    @Mock
    private AuthorizationResponseHandler authorizationResponseHandler;
    @Mock
    private AuthorizationRequest authorizationRequest;

    @Test
    void sendToPermissionAdministrator_changesState_andCallsApi() throws StateTransitionException {
        // Given
        var permissionId = "SomeId";
        var connectionId = "bar";
        var dataNeedId = "luu";
        var nif = "muh";
        var meteringPointId = "kuh";
        var now = ZonedDateTime.now();
        var requestDataFrom = now.minusDays(10);
        var requestDataTo = now.minusDays(5);

        var permissionRequest = new DatadisPermissionRequest(permissionId, connectionId, dataNeedId,
                nif, meteringPointId, MeasurementType.QUARTER_HOURLY, requestDataFrom, requestDataTo,
                mock(AuthorizationApi.class), mock(AuthorizationResponseHandler.class));
        var validatedState = new ValidatedState(permissionRequest, authorizationRequest, authorizationApi, authorizationResponseHandler);
        permissionRequest.changeState(validatedState);

        when(authorizationApi.postAuthorizationRequest(any())).thenReturn(Mono.just(AuthorizationRequestResponse.OK));

        // When
        assertDoesNotThrow(validatedState::sendToPermissionAdministrator);

        // Then
        assertEquals(PermissionProcessStatus.PENDING_PERMISSION_ADMINISTRATOR_ACKNOWLEDGEMENT, permissionRequest.state().status());
        verify(authorizationApi).postAuthorizationRequest(authorizationRequest);
        verify(authorizationResponseHandler).handleAuthorizationRequestResponse(eq(permissionId), any());
    }
}