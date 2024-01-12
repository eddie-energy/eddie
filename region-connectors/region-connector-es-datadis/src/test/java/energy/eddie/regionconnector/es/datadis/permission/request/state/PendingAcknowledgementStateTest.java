package energy.eddie.regionconnector.es.datadis.permission.request.state;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequestResponse;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.ZONE_ID_SPAIN;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith({MockitoExtension.class})
class PendingAcknowledgementStateTest {

    @Mock
    private AuthorizationApi authorizationApi;

    private DatadisPermissionRequest makePendingPermissionRequest() {
        var now = ZonedDateTime.now(ZONE_ID_SPAIN);
        var requestDataFrom = now.minusDays(10);
        var requestDataTo = now.minusDays(5);

        var requestForCreation = new PermissionRequestForCreation("bar", "luu", "muh", "kuh", requestDataFrom, requestDataTo, MeasurementType.QUARTER_HOURLY);
        var permissionRequest = new DatadisPermissionRequest("SomeId", requestForCreation, authorizationApi);
        var pendingAcknowledgementState = new PendingAcknowledgementState(permissionRequest, AuthorizationRequestResponse.OK);
        permissionRequest.changeState(pendingAcknowledgementState);
        return permissionRequest;
    }

    @Test
    void receivedPermissionAdministratorResponse_changesStateToSentToPA() throws StateTransitionException {
        var permissionRequest = makePendingPermissionRequest();

        permissionRequest.receivedPermissionAdministratorResponse();
        assertEquals(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR, permissionRequest.state().status());
    }
}