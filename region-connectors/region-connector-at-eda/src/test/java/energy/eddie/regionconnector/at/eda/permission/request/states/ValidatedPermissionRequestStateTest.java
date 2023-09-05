package energy.eddie.regionconnector.at.eda.permission.request.states;

import at.ebutilities.schemata.customerconsent.cmrequest._01p10.CMRequest;
import energy.eddie.regionconnector.at.api.FutureStateException;
import energy.eddie.regionconnector.at.api.PastStateException;
import energy.eddie.regionconnector.at.api.PermissionRequest;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.TransmissionException;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import jakarta.xml.bind.JAXBException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class ValidatedPermissionRequestStateTest {

    @Test
    void sendToPermissionAdministrator_transitionsToPendingState() {
        // Given
        EdaAdapter edaAdapter = mock(EdaAdapter.class);
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        PermissionRequest permissionRequest = new EdaPermissionRequest("connectionId", ccmoRequest, null);
        ValidatedPermissionRequestState state = new ValidatedPermissionRequestState(permissionRequest, new CMRequest(), edaAdapter);
        permissionRequest.changeState(state);

        // When
        state.sendToPermissionAdministrator();

        // Then
        assertEquals(PendingAcknowledgmentPermissionRequestState.class, permissionRequest.state().getClass());
    }

    @Test
    void sendToPermissionAdministrator_changesState_whenNotSerializableToJson() throws TransmissionException, JAXBException {
        // Given
        EdaAdapter edaAdapter = mock(EdaAdapter.class);
        doThrow(new JAXBException("msg")).when(edaAdapter).sendCMRequest(any());
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        PermissionRequest permissionRequest = new EdaPermissionRequest("connectionId", ccmoRequest, null);
        ValidatedPermissionRequestState state = new ValidatedPermissionRequestState(permissionRequest, new CMRequest(), edaAdapter);
        permissionRequest.changeState(state);

        // When
        state.sendToPermissionAdministrator();

        // Then
        assertEquals(UnableToSendPermissionRequestState.class, permissionRequest.state().getClass());
    }

    @Test
    void sendToPermissionAdministrator_changesState_whenUnableToSend() throws TransmissionException, JAXBException {
        // Given
        EdaAdapter edaAdapter = mock(EdaAdapter.class);
        doThrow(new TransmissionException(new Exception())).when(edaAdapter).sendCMRequest(any());
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        PermissionRequest permissionRequest = new EdaPermissionRequest("connectionId", ccmoRequest, null);
        ValidatedPermissionRequestState state = new ValidatedPermissionRequestState(permissionRequest, new CMRequest(), edaAdapter);
        permissionRequest.changeState(state);

        // When
        state.sendToPermissionAdministrator();

        // Then
        assertEquals(UnableToSendPermissionRequestState.class, permissionRequest.state().getClass());
    }

    @Test
    void validate_throws() {
        // Given
        ValidatedPermissionRequestState state = new ValidatedPermissionRequestState(null, null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::validate);
    }

    @Test
    void receivedPermissionAdministratorResponse_throws() {
        // Given
        ValidatedPermissionRequestState state = new ValidatedPermissionRequestState(null, null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::receivedPermissionAdministratorResponse);
    }

    @Test
    void accept_throws() {
        // Given
        ValidatedPermissionRequestState state = new ValidatedPermissionRequestState(null, null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::accept);
    }

    @Test
    void invalid_throws() {
        // Given
        ValidatedPermissionRequestState state = new ValidatedPermissionRequestState(null, null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::invalid);
    }

    @Test
    void reject_throws() {
        // Given
        ValidatedPermissionRequestState state = new ValidatedPermissionRequestState(null, null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::reject);
    }

    @Test
    void terminate_throws() {
        // Given
        ValidatedPermissionRequestState state = new ValidatedPermissionRequestState(null, null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::terminate);
    }
}