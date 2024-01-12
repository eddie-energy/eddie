package energy.eddie.regionconnector.es.datadis.permission.request.state;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequest;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequestResponse;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.ZONE_ID_SPAIN;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
class ValidatedStateTest {
    @Mock
    private AuthorizationRequest authorizationRequest;
    @Mock
    private AuthorizationApi authorizationApi;

    private DatadisPermissionRequest makeValidatedPermissionRequest(String permissionId) {
        var now = ZonedDateTime.now(ZONE_ID_SPAIN);
        var requestDataFrom = now.minusDays(10);
        var requestDataTo = now.minusDays(5);

        var requestForCreation = new PermissionRequestForCreation("bar", "luu", "muh", "kuh", requestDataFrom, requestDataTo, MeasurementType.QUARTER_HOURLY);
        var permissionRequest = new DatadisPermissionRequest(permissionId, requestForCreation, authorizationApi);
        var validatedState = new ValidatedState(permissionRequest, authorizationRequest, authorizationApi);
        permissionRequest.changeState(validatedState);
        return permissionRequest;
    }

    @ParameterizedTest
    @EnumSource(value = AuthorizationRequestResponse.class)
    void sendToPermissionAdministrator_noError_changesStateToPending(AuthorizationRequestResponse response) {
        // Given
        var permissionId = "SomeId";
        var permissionRequest = makeValidatedPermissionRequest(permissionId);
        when(authorizationApi.postAuthorizationRequest(any())).thenReturn(Mono.just(response));

        // When
        assertDoesNotThrow(permissionRequest::sendToPermissionAdministrator);

        // Then before response is received
        assertEquals(PermissionProcessStatus.PENDING_PERMISSION_ADMINISTRATOR_ACKNOWLEDGEMENT, permissionRequest.state().status());
        verify(authorizationApi).postAuthorizationRequest(authorizationRequest);
    }

    @Test
    void sendToPermissionAdministrator_onError_changesStateToUnableToSend() {
        // Given
        var permissionId = "SomeId";
        var permissionRequest = makeValidatedPermissionRequest(permissionId);
        when(authorizationApi.postAuthorizationRequest(any())).thenReturn(Mono.error(new RuntimeException()));

        // When
        assertDoesNotThrow(permissionRequest::sendToPermissionAdministrator);

        // Then before response is received
        assertEquals(PermissionProcessStatus.UNABLE_TO_SEND, permissionRequest.state().status());
        verify(authorizationApi).postAuthorizationRequest(authorizationRequest);
    }
}