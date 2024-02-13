package energy.eddie.regionconnector.at.eda.permission.request.states;

import energy.eddie.api.agnostic.process.model.FutureStateException;
import energy.eddie.api.agnostic.process.model.PastStateException;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.StateBuilderFactory;
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
        var factory = new StateBuilderFactory(mock(AtConfiguration.class), mock(EdaAdapter.class));
        var permissionRequest = new EdaPermissionRequest("connectionId", "dataNeedId", ccmoRequest, factory);
        AtSentToPermissionAdministratorPermissionRequestState state = new AtSentToPermissionAdministratorPermissionRequestState(permissionRequest, factory);
        permissionRequest.changeState(state);

        // When
        state.accept();

        // Then
        assertEquals(AtAcceptedPermissionRequestState.class, permissionRequest.state().getClass());
    }

    @Test
    void invalid_transitionsToInvalidState() {
        // Given
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        var factory = new StateBuilderFactory(mock(AtConfiguration.class), mock(EdaAdapter.class));
        var permissionRequest = new EdaPermissionRequest("connectionId", "dataNeedId", ccmoRequest, factory);
        AtSentToPermissionAdministratorPermissionRequestState state = new AtSentToPermissionAdministratorPermissionRequestState(permissionRequest, factory);
        permissionRequest.changeState(state);

        // When
        state.invalid();

        // Then
        assertEquals(AtInvalidPermissionRequestState.class, permissionRequest.state().getClass());
    }

    @Test
    void reject_transitionsToRejectedState() {
        // Given
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        var factory = new StateBuilderFactory(mock(AtConfiguration.class), mock(EdaAdapter.class));
        var permissionRequest = new EdaPermissionRequest("connectionId", "dataNeedId", ccmoRequest, factory);
        AtSentToPermissionAdministratorPermissionRequestState state = new AtSentToPermissionAdministratorPermissionRequestState(permissionRequest, factory);
        permissionRequest.changeState(state);

        // When
        state.reject();

        // Then
        assertEquals(AtRejectedPermissionRequestState.class, permissionRequest.state().getClass());
    }

    @Test
    void validate_throws() {
        // Given
        AtSentToPermissionAdministratorPermissionRequestState state = new AtSentToPermissionAdministratorPermissionRequestState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::validate);
    }

    @Test
    void sendToPermissionAdministrator_throws() {
        // Given
        AtSentToPermissionAdministratorPermissionRequestState state = new AtSentToPermissionAdministratorPermissionRequestState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::sendToPermissionAdministrator);
    }

    @Test
    void receivedPermissionAdministratorResponse_throws() {
        // Given
        AtSentToPermissionAdministratorPermissionRequestState state = new AtSentToPermissionAdministratorPermissionRequestState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::receivedPermissionAdministratorResponse);
    }

    @Test
    void terminate_throws() {
        // Given
        AtSentToPermissionAdministratorPermissionRequestState state = new AtSentToPermissionAdministratorPermissionRequestState(null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::terminate);
    }

}