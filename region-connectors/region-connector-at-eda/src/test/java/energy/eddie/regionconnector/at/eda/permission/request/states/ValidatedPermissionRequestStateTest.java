package energy.eddie.regionconnector.at.eda.permission.request.states;

import at.ebutilities.schemata.customerconsent.cmrequest._01p10.CMRequest;
import energy.eddie.api.agnostic.process.model.FutureStateException;
import energy.eddie.api.agnostic.process.model.PastStateException;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.TransmissionException;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.StateBuilderFactory;
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
        var factory = new StateBuilderFactory(mock(AtConfiguration.class), edaAdapter);
        var permissionRequest = new EdaPermissionRequest("connectionId", "dataNeedId", ccmoRequest, factory);
        AtValidatedPermissionRequestState state = new AtValidatedPermissionRequestState(permissionRequest, new CMRequest(), edaAdapter, factory);
        permissionRequest.changeState(state);

        // When
        state.sendToPermissionAdministrator();

        // Then
        assertEquals(AtPendingAcknowledgmentPermissionRequestState.class, permissionRequest.state().getClass());
    }

    @Test
    void sendToPermissionAdministrator_changesState_whenNotSerializableToJson() throws TransmissionException, JAXBException {
        // Given
        EdaAdapter edaAdapter = mock(EdaAdapter.class);
        doThrow(new JAXBException("msg")).when(edaAdapter).sendCMRequest(any());
        var factory = new StateBuilderFactory(mock(AtConfiguration.class), edaAdapter);
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        var permissionRequest = new EdaPermissionRequest("connectionId", "dataNeedId", ccmoRequest, factory);
        AtValidatedPermissionRequestState state = new AtValidatedPermissionRequestState(permissionRequest, new CMRequest(), edaAdapter, factory);
        permissionRequest.changeState(state);

        // When
        state.sendToPermissionAdministrator();

        // Then
        assertEquals(AtUnableToSendPermissionRequestState.class, permissionRequest.state().getClass());
    }

    @Test
    void sendToPermissionAdministrator_changesState_whenUnableToSend() throws TransmissionException, JAXBException {
        // Given
        EdaAdapter edaAdapter = mock(EdaAdapter.class);
        doThrow(new TransmissionException(new Exception())).when(edaAdapter).sendCMRequest(any());
        var factory = new StateBuilderFactory(mock(AtConfiguration.class), edaAdapter);
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        var permissionRequest = new EdaPermissionRequest("connectionId", "dataNeedId", ccmoRequest, factory);
        AtValidatedPermissionRequestState state = new AtValidatedPermissionRequestState(permissionRequest, new CMRequest(), edaAdapter, factory);
        permissionRequest.changeState(state);

        // When
        state.sendToPermissionAdministrator();

        // Then
        assertEquals(AtUnableToSendPermissionRequestState.class, permissionRequest.state().getClass());
    }

    @Test
    void validate_throws() {
        // Given
        AtValidatedPermissionRequestState state = new AtValidatedPermissionRequestState(null, null, null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::validate);
    }

    @Test
    void receivedPermissionAdministratorResponse_throws() {
        // Given
        AtValidatedPermissionRequestState state = new AtValidatedPermissionRequestState(null, null, null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::receivedPermissionAdministratorResponse);
    }

    @Test
    void accept_throws() {
        // Given
        AtValidatedPermissionRequestState state = new AtValidatedPermissionRequestState(null, null, null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::accept);
    }

    @Test
    void invalid_throws() {
        // Given
        AtValidatedPermissionRequestState state = new AtValidatedPermissionRequestState(null, null, null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::invalid);
    }

    @Test
    void reject_throws() {
        // Given
        AtValidatedPermissionRequestState state = new AtValidatedPermissionRequestState(null, null, null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::reject);
    }

    @Test
    void terminate_throws() {
        // Given
        AtValidatedPermissionRequestState state = new AtValidatedPermissionRequestState(null, null, null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::terminate);
    }
}