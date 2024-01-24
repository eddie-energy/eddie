package energy.eddie.regionconnector.aiida.states;

import energy.eddie.api.agnostic.process.model.FutureStateException;
import energy.eddie.api.agnostic.process.model.PastStateException;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.aiida.permission.request.AiidaPermissionRequest;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AiidaSentToPermissionAdministratorPermissionRequestStateTest {
    @Test
    void status_returnsSentToPermissionAdministrator() {
        // Given
        AiidaSentToPermissionAdministratorPermissionRequestState state = new AiidaSentToPermissionAdministratorPermissionRequestState(null);

        // When
        // Then
        assertEquals(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR, state.status());
    }

    @Test
    void accept_changesToAcceptedState() {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        AiidaPermissionRequest permissionRequest = new AiidaPermissionRequest("foo", "bar",
                "loo", "too", now, now);
        var state = new AiidaSentToPermissionAdministratorPermissionRequestState(permissionRequest);
        permissionRequest.changeState(state);

        // When
        assertDoesNotThrow(permissionRequest::accept);

        // Then
        assertEquals(AiidaAcceptedPermissionRequestState.class, permissionRequest.state().getClass());
    }

    @Test
    void invalid_changesToInvalidState() {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        AiidaPermissionRequest permissionRequest = new AiidaPermissionRequest("foo", "bar",
                "loo", "too", now, now);
        var state = new AiidaSentToPermissionAdministratorPermissionRequestState(permissionRequest);
        permissionRequest.changeState(state);

        // When
        assertDoesNotThrow(permissionRequest::invalid);

        // Then
        assertEquals(AiidaInvalidPermissionRequestState.class, permissionRequest.state().getClass());
    }

    @Test
    void reject_throwsUnsupportedOperationException() {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        AiidaPermissionRequest permissionRequest = new AiidaPermissionRequest("foo", "bar",
                "loo", "too", now, now);
        var state = new AiidaSentToPermissionAdministratorPermissionRequestState(permissionRequest);
        permissionRequest.changeState(state);

        // When
        assertThrows(UnsupportedOperationException.class, permissionRequest::reject);
    }

    @Test
    void timeOut_notImplemented() {
        // Given
        AiidaSentToPermissionAdministratorPermissionRequestState state = new AiidaSentToPermissionAdministratorPermissionRequestState(null);

        // When
        // Then
        assertThrows(UnsupportedOperationException.class, state::timeOut);
    }

    @Test
    void validate_throws() {
        // Given
        AiidaSentToPermissionAdministratorPermissionRequestState state = new AiidaSentToPermissionAdministratorPermissionRequestState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::validate);
    }

    @Test
    void sendToPermissionAdministrator_throws() {
        // Given
        AiidaSentToPermissionAdministratorPermissionRequestState state = new AiidaSentToPermissionAdministratorPermissionRequestState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::sendToPermissionAdministrator);
    }

    @Test
    void receivedPermissionAdministratorResponse_throws() {
        // Given
        AiidaSentToPermissionAdministratorPermissionRequestState state = new AiidaSentToPermissionAdministratorPermissionRequestState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::receivedPermissionAdministratorResponse);
    }

    @Test
    void terminate_throws() {
        // Given
        AiidaSentToPermissionAdministratorPermissionRequestState state = new AiidaSentToPermissionAdministratorPermissionRequestState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::terminate);
    }

    @Test
    void revoke_throws() {
        // Given
        AiidaSentToPermissionAdministratorPermissionRequestState state = new AiidaSentToPermissionAdministratorPermissionRequestState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::revoke);
    }

    @Test
    void timeLimit_throws() {
        // Given
        AiidaSentToPermissionAdministratorPermissionRequestState state = new AiidaSentToPermissionAdministratorPermissionRequestState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::fulfill);
    }
}