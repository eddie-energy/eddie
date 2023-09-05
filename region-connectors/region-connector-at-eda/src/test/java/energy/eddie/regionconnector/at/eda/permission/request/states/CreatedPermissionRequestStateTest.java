package energy.eddie.regionconnector.at.eda.permission.request.states;

import at.ebutilities.schemata.customerconsent.cmrequest._01p10.CMRequest;
import energy.eddie.api.v0.process.model.FutureStateException;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import energy.eddie.regionconnector.at.eda.requests.InvalidDsoIdException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CreatedPermissionRequestStateTest {

    @Test
    void createdStated_changesToValidated_whenValid() throws InvalidDsoIdException {
        // Given
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.toCMRequest()).thenReturn(mock(CMRequest.class));
        AtPermissionRequest permissionRequest = new EdaPermissionRequest("connectionId", ccmoRequest, null);
        CreatedPermissionRequestState permissionRequestState = new CreatedPermissionRequestState(permissionRequest, ccmoRequest, null);

        // When
        permissionRequestState.validate();

        // Then
        assertEquals(ValidatedPermissionRequestState.class, permissionRequest.state().getClass());
    }

    @Test
    void createdStated_changesToMalformed_whenExceptionOccurs() throws InvalidDsoIdException {
        // Given
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.toCMRequest()).thenThrow(new InvalidDsoIdException("msg"));
        AtPermissionRequest permissionRequest = new EdaPermissionRequest("connectionId", ccmoRequest, null);
        CreatedPermissionRequestState permissionRequestState = new CreatedPermissionRequestState(permissionRequest, ccmoRequest, null);

        // When
        permissionRequestState.validate();

        // Then
        assertEquals(MalformedPermissionRequestState.class, permissionRequest.state().getClass());
    }


    @Test
    void sendToPermissionAdministrator_throws() {
        // Given
        CreatedPermissionRequestState state = new CreatedPermissionRequestState(null, null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::sendToPermissionAdministrator);
    }

    @Test
    void receivedPermissionAdministratorResponse_throws() {
        // Given
        CreatedPermissionRequestState state = new CreatedPermissionRequestState(null, null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::receivedPermissionAdministratorResponse);
    }

    @Test
    void accept_throws() {
        // Given
        CreatedPermissionRequestState state = new CreatedPermissionRequestState(null, null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::accept);
    }

    @Test
    void invalid_throws() {
        // Given
        CreatedPermissionRequestState state = new CreatedPermissionRequestState(null, null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::invalid);
    }

    @Test
    void reject_throws() {
        // Given
        CreatedPermissionRequestState state = new CreatedPermissionRequestState(null, null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::reject);
    }

    @Test
    void terminate_throws() {
        // Given
        CreatedPermissionRequestState state = new CreatedPermissionRequestState(null, null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::terminate);
    }

}