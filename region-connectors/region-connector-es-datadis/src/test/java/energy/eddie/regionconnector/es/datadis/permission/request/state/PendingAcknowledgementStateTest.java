package energy.eddie.regionconnector.es.datadis.permission.request.state;

import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequestResponse;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.ZONE_ID_SPAIN;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith({MockitoExtension.class})
class PendingAcknowledgementStateTest {

    @Mock
    private AuthorizationApi authorizationApi;

    private DatadisPermissionRequest makePendingPermissionRequest(AuthorizationRequestResponse response) {
        var now = ZonedDateTime.now(ZONE_ID_SPAIN);
        var requestDataFrom = now.minusDays(10);
        var requestDataTo = now.minusDays(5);

        var requestForCreation = new PermissionRequestForCreation("bar", "luu", "muh", "kuh", requestDataFrom, requestDataTo, MeasurementType.QUARTER_HOURLY);
        var permissionRequest = new DatadisPermissionRequest("SomeId", requestForCreation, authorizationApi);
        var pendingAcknowledgementState = new PendingAcknowledgementState(permissionRequest, response);
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