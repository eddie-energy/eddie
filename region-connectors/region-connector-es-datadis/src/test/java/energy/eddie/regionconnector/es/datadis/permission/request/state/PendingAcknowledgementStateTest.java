package energy.eddie.regionconnector.es.datadis.permission.request.state;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequestResponse;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.StateBuilderFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.ZONE_ID_SPAIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@ExtendWith({MockitoExtension.class})
class PendingAcknowledgementStateTest {
    final StateBuilderFactory factory = new StateBuilderFactory(mock(AuthorizationApi.class));

    private DatadisPermissionRequest makePendingPermissionRequest(AuthorizationRequestResponse response) {
        var now = LocalDate.now(ZONE_ID_SPAIN);
        var requestDataFrom = now.minusDays(10);
        var requestDataTo = now.minusDays(5);

        var requestForCreation = new PermissionRequestForCreation("bar", "luu", "muh", "kuh", requestDataFrom, requestDataTo, Granularity.PT15M);
        var permissionRequest = new DatadisPermissionRequest("SomeId", requestForCreation, factory);
        var pendingAcknowledgementState = new PendingAcknowledgementState(permissionRequest, response, factory);
        permissionRequest.changeState(pendingAcknowledgementState);
        return permissionRequest;
    }

    @Test
    void receivedPermissionAdministratorResponse_withOkResponse_changesStateToSentToPA() throws StateTransitionException {
        var permissionRequest = makePendingPermissionRequest(AuthorizationRequestResponse.fromResponse("ok"));

        permissionRequest.receivedPermissionAdministratorResponse();
        assertEquals(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR, permissionRequest.state().status());
    }

    @ParameterizedTest
    @ValueSource(strings = {"nonif", "nopermisos", "unknown", "xxx"})
    void receivedPermissionAdministratorResponse_withNoOk_changesStateToInvalid(String response) throws StateTransitionException {
        var permissionRequest = makePendingPermissionRequest(AuthorizationRequestResponse.fromResponse(response));

        permissionRequest.receivedPermissionAdministratorResponse();
        assertEquals(PermissionProcessStatus.INVALID, permissionRequest.state().status());
    }
}
