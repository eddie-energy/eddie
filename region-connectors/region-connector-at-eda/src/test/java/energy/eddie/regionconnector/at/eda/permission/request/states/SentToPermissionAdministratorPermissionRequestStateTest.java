package energy.eddie.regionconnector.at.eda.permission.request.states;

import energy.eddie.regionconnector.at.api.FutureStateException;
import energy.eddie.regionconnector.at.api.PastStateException;
import energy.eddie.regionconnector.at.api.PermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SentToPermissionAdministratorPermissionRequestStateTest {

    @Test
    void accept_transitionsToAcceptedState() {
        // Given
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        PermissionRequest permissionRequest = new EdaPermissionRequest("connectionId", ccmoRequest, null);
        SentToPermissionAdministratorPermissionRequestState state = new SentToPermissionAdministratorPermissionRequestState(permissionRequest);
        permissionRequest.changeState(state);

        // When
        state.accept();

        // Then
        assertEquals(AcceptedPermissionRequestState.class, permissionRequest.state().getClass());
    }

    @Test
    void invalid_transitionsToInvalidState() {
        // Given
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        PermissionRequest permissionRequest = new EdaPermissionRequest("connectionId", ccmoRequest, null);
        SentToPermissionAdministratorPermissionRequestState state = new SentToPermissionAdministratorPermissionRequestState(permissionRequest);
        permissionRequest.changeState(state);

        // When
        state.invalid();

        // Then
        assertEquals(InvalidPermissionRequestState.class, permissionRequest.state().getClass());
    }

    @Test
    void reject_transitionsToRejectedState() {
        // Given
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        PermissionRequest permissionRequest = new EdaPermissionRequest("connectionId", ccmoRequest, null);
        SentToPermissionAdministratorPermissionRequestState state = new SentToPermissionAdministratorPermissionRequestState(permissionRequest);
        permissionRequest.changeState(state);

        // When
        state.reject();

        // Then
        assertEquals(RejectedPermissionRequestState.class, permissionRequest.state().getClass());
    }

    @Test
    void validate_throws() {
        // Given
        SentToPermissionAdministratorPermissionRequestState state = new SentToPermissionAdministratorPermissionRequestState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::validate);
    }

    @Test
    void sendToPermissionAdministrator_throws() {
        // Given
        SentToPermissionAdministratorPermissionRequestState state = new SentToPermissionAdministratorPermissionRequestState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::sendToPermissionAdministrator);
    }

    @Test
    void receivedPermissionAdministratorResponse_throws() {
        // Given
        SentToPermissionAdministratorPermissionRequestState state = new SentToPermissionAdministratorPermissionRequestState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::receivedPermissionAdministratorResponse);
    }

    @Test
    void terminate_throws() {
        // Given
        SentToPermissionAdministratorPermissionRequestState state = new SentToPermissionAdministratorPermissionRequestState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::terminate);
    }

}