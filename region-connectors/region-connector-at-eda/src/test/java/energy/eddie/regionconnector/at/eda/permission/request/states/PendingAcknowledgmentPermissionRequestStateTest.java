package energy.eddie.regionconnector.at.eda.permission.request.states;

import energy.eddie.api.agnostic.process.model.FutureStateException;
import energy.eddie.api.agnostic.process.model.PastStateException;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PendingAcknowledgmentPermissionRequestStateTest {

    @Test
    void pendingAcknowledgmentPermissionRequestState_transitionsToSentState() {
        // Given
        EdaAdapter edaAdapter = mock(EdaAdapter.class);
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("conversationId");
        var permissionRequest = new EdaPermissionRequest("connectionId", "dataNeedId", ccmoRequest, edaAdapter);
        AtPendingAcknowledgmentPermissionRequestState state = new AtPendingAcknowledgmentPermissionRequestState(permissionRequest);
        permissionRequest.changeState(state);

        // When
        state.receivedPermissionAdministratorResponse();

        // Then
        assertEquals(AtSentToPermissionAdministratorPermissionRequestState.class, permissionRequest.state().getClass());
    }

    @Test
    void validate_throws() {
        // Given
        AtPendingAcknowledgmentPermissionRequestState state = new AtPendingAcknowledgmentPermissionRequestState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::validate);
    }

    @Test
    void sendToPermissionAdministrator_throws() {
        // Given
        AtPendingAcknowledgmentPermissionRequestState state = new AtPendingAcknowledgmentPermissionRequestState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::sendToPermissionAdministrator);
    }

    @Test
    void accept_throws() {
        // Given
        AtPendingAcknowledgmentPermissionRequestState state = new AtPendingAcknowledgmentPermissionRequestState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::accept);
    }

    @Test
    void invalid_throws() {
        // Given
        AtPendingAcknowledgmentPermissionRequestState state = new AtPendingAcknowledgmentPermissionRequestState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::invalid);
    }

    @Test
    void reject_throws() {
        // Given
        AtPendingAcknowledgmentPermissionRequestState state = new AtPendingAcknowledgmentPermissionRequestState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::reject);
    }

    @Test
    void terminate_throws() {
        // Given
        AtPendingAcknowledgmentPermissionRequestState state = new AtPendingAcknowledgmentPermissionRequestState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::terminate);
    }
}