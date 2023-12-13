package energy.eddie.regionconnector.es.datadis.permission.request.state;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationResponseHandler;
import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequest;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequestResponse;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.ZONE_ID_SPAIN;
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
    void sendToPermissionAdministrator_changesState_andCallsApi() {
        // Given
        var permissionId = "SomeId";
        var connectionId = "bar";
        var dataNeedId = "luu";
        var nif = "muh";
        var meteringPointId = "kuh";
        var now = ZonedDateTime.now(ZONE_ID_SPAIN);
        var requestDataFrom = now.minusDays(10);
        var requestDataTo = now.minusDays(5);

        var requestForCreation = new PermissionRequestForCreation(connectionId, dataNeedId, nif, meteringPointId,
                requestDataFrom, requestDataTo, MeasurementType.QUARTER_HOURLY);
        var permissionRequest = new DatadisPermissionRequest(permissionId, requestForCreation,
                authorizationApi, authorizationResponseHandler);
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