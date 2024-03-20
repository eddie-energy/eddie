package energy.eddie.regionconnector.es.datadis.permission.request.state;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequestFactory;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequestResponse;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.StateBuilderFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
class ValidatedStateTest {
    @Mock
    private AuthorizationApi authorizationApi;
    private final StateBuilderFactory factory = new StateBuilderFactory(authorizationApi);

    private DatadisPermissionRequest makeValidatedPermissionRequest(String permissionId) {
        var now = LocalDate.now(ZONE_ID_SPAIN);
        var requestDataFrom = now.minusDays(10);
        var requestDataTo = now.minusDays(5);

        var requestForCreation = new PermissionRequestForCreation("bar", "luu", "muh", "kuh");
        var permissionRequest = new DatadisPermissionRequest(permissionId,
                                                             requestForCreation,
                                                             requestDataFrom,
                                                             requestDataTo,
                                                             Granularity.PT15M,
                                                             factory);
        var validatedState = new ValidatedState(permissionRequest,
                                                authorizationApi,
                                                new AuthorizationRequestFactory(),
                                                factory);
        permissionRequest.changeState(validatedState);
        return permissionRequest;
    }

    @ParameterizedTest
    @ValueSource(strings = {"ok", "no_nif", "no_supplies", "unknown", "xxx"})
    void sendToPermissionAdministrator_noError_changesStateToPending(String response) {
        // Given
        var permissionId = "SomeId";
        var permissionRequest = makeValidatedPermissionRequest(permissionId);
        when(authorizationApi.postAuthorizationRequest(any())).thenReturn(Mono.just(AuthorizationRequestResponse.fromResponse(
                response)));

        // When
        assertDoesNotThrow(permissionRequest::sendToPermissionAdministrator);

        // Then before response is received
        assertEquals(PermissionProcessStatus.PENDING_PERMISSION_ADMINISTRATOR_ACKNOWLEDGEMENT,
                     permissionRequest.state().status());
        verify(authorizationApi).postAuthorizationRequest(any());
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
        verify(authorizationApi).postAuthorizationRequest(any());
    }
}
