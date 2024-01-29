package energy.eddie.regionconnector.at.eda.permission.request.states;

import at.ebutilities.schemata.customerconsent.cmrequest._01p10.CMRequest;
import energy.eddie.api.agnostic.process.model.FutureStateException;
import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.api.agnostic.process.model.validation.ValidationException;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.StateBuilderFactory;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import energy.eddie.regionconnector.at.eda.requests.InvalidDsoIdException;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CreatedPermissionRequestStateTest {

    @Test
    void createdStated_changesToValidated_whenValid() throws InvalidDsoIdException, ValidationException {
        // Given
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.start()).thenReturn(ZonedDateTime.now(ZoneOffset.UTC).minusDays(5));
        when(ccmoRequest.end()).thenReturn(Optional.of(ZonedDateTime.now(ZoneOffset.UTC).minusDays(1)));
        when(ccmoRequest.toCMRequest()).thenReturn(mock(CMRequest.class));
        var factory = new StateBuilderFactory(mock(AtConfiguration.class), mock(EdaAdapter.class));
        AtPermissionRequest permissionRequest = new EdaPermissionRequest("connectionId", "dataNeedId", ccmoRequest, factory);
        AtCreatedPermissionRequestState permissionRequestState = new AtCreatedPermissionRequestState(permissionRequest, ccmoRequest, factory);

        // When
        permissionRequestState.validate();

        // Then
        assertEquals(AtValidatedPermissionRequestState.class, permissionRequest.state().getClass());
    }

    @Test
    void createdStated_changesToMalformed_whenExceptionOccurs() throws InvalidDsoIdException {
        // Given
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.start()).thenReturn(ZonedDateTime.now(ZoneOffset.UTC).minusDays(5));
        when(ccmoRequest.end()).thenReturn(Optional.of(ZonedDateTime.now(ZoneOffset.UTC).minusDays(1)));
        when(ccmoRequest.toCMRequest()).thenThrow(new InvalidDsoIdException("msg"));
        var factory = new StateBuilderFactory(mock(AtConfiguration.class), mock(EdaAdapter.class));
        AtPermissionRequest permissionRequest = new EdaPermissionRequest("connectionId", "dataNeedId", ccmoRequest, factory);
        AtCreatedPermissionRequestState permissionRequestState = new AtCreatedPermissionRequestState(permissionRequest, ccmoRequest, factory);

        // When
        assertThrows(StateTransitionException.class, permissionRequestState::validate);

        // Then
        assertEquals(AtMalformedPermissionRequestState.class, permissionRequest.state().getClass());
    }


    @Test
    void sendToPermissionAdministrator_throws() {
        // Given
        AtCreatedPermissionRequestState state = new AtCreatedPermissionRequestState(null, null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::sendToPermissionAdministrator);
    }

    @Test
    void receivedPermissionAdministratorResponse_throws() {
        // Given
        AtCreatedPermissionRequestState state = new AtCreatedPermissionRequestState(null, null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::receivedPermissionAdministratorResponse);
    }

    @Test
    void accept_throws() {
        // Given
        AtCreatedPermissionRequestState state = new AtCreatedPermissionRequestState(null, null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::accept);
    }

    @Test
    void invalid_throws() {
        // Given
        AtCreatedPermissionRequestState state = new AtCreatedPermissionRequestState(null, null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::invalid);
    }

    @Test
    void reject_throws() {
        // Given
        AtCreatedPermissionRequestState state = new AtCreatedPermissionRequestState(null, null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::reject);
    }

    @Test
    void terminate_throws() {
        // Given
        AtCreatedPermissionRequestState state = new AtCreatedPermissionRequestState(null, null, null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::terminate);
    }

}