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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.ZONE_ID_SPAIN;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith({MockitoExtension.class})
class SentToPermissionAdministratorStateTest {

    @Mock
    private AuthorizationApi authorizationApi;

    private DatadisPermissionRequest makePermissionRequest() {
        var now = ZonedDateTime.now(ZONE_ID_SPAIN);
        var requestDataFrom = now.minusDays(10);
        var requestDataTo = now.minusDays(5);

        var requestForCreation = new PermissionRequestForCreation("bar", "luu", "muh", "kuh", requestDataFrom, requestDataTo, MeasurementType.QUARTER_HOURLY);
        return new DatadisPermissionRequest("SomeId", requestForCreation, authorizationApi);
    }

    private DatadisPermissionRequest makeSentPermissionRequest() {
        var permissionRequest = makePermissionRequest();
        permissionRequest.changeState(new SentToPermissionAdministratorState(permissionRequest, AuthorizationRequestResponse.OK));
        return permissionRequest;
    }


    @Test
    void creatingState_withNoNifResponse_changesStateToInvalid() {
        var permissionRequest = makePermissionRequest();

        new SentToPermissionAdministratorState(permissionRequest, AuthorizationRequestResponse.NO_NIF);

        assertEquals(PermissionProcessStatus.INVALID, permissionRequest.state().status());
    }

    @Test
    void creatingState_withNoSuppliesResponse_changesStateToInvalid() {
        var permissionRequest = makePermissionRequest();

        new SentToPermissionAdministratorState(permissionRequest, AuthorizationRequestResponse.NO_SUPPLIES);

        assertEquals(PermissionProcessStatus.INVALID, permissionRequest.state().status());
    }

    @Test
    void creatingState_withOkResponse_doesNotChangeState() {
        var permissionRequest = makeSentPermissionRequest();

        new SentToPermissionAdministratorState(permissionRequest, AuthorizationRequestResponse.OK);

        assertEquals(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR, permissionRequest.state().status());
    }

    @Test
    void accept_changesToAccepted() throws StateTransitionException {
        var permissionRequest = makeSentPermissionRequest();

        permissionRequest.accept();

        assertEquals(PermissionProcessStatus.ACCEPTED, permissionRequest.state().status());
    }

    @Test
    void reject_changesToRejected() throws StateTransitionException {
        var permissionRequest = makeSentPermissionRequest();

        permissionRequest.reject();

        assertEquals(PermissionProcessStatus.REJECTED, permissionRequest.state().status());
    }

    @Test
    void invalid_changesToInvalid() throws StateTransitionException {
        var permissionRequest = makeSentPermissionRequest();

        permissionRequest.invalid();

        assertEquals(PermissionProcessStatus.INVALID, permissionRequest.state().status());
    }
}