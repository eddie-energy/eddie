package energy.eddie.regionconnector.aiida.states;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.process.model.FutureStateException;
import energy.eddie.api.v0.process.model.PastStateException;
import energy.eddie.regionconnector.aiida.permission.request.AiidaPermissionRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AiidaValidatedPermissionRequestStateTest {
    @Test
    void status_returnsValidated() {
        // Given
        AiidaValidatedPermissionRequestState state = new AiidaValidatedPermissionRequestState(null);

        // When
        // Then
        assertEquals(PermissionProcessStatus.VALIDATED, state.status());
    }

    @Test
    void validate_throws() {
        // Given
        AiidaValidatedPermissionRequestState state = new AiidaValidatedPermissionRequestState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::validate);
    }

    @Test
    void sendToPermissionAdministrator_changesToSentToPermissionAdministrator() {
        // Given
        var now = Instant.now();
        AiidaPermissionRequest permissionRequest = new AiidaPermissionRequest("foo", "bar",
                "loo", "too", now, now);
        var state = new AiidaValidatedPermissionRequestState(permissionRequest);
        permissionRequest.changeState(state);

        // When
        assertDoesNotThrow(permissionRequest::sendToPermissionAdministrator);

        // Then
        assertEquals(AiidaSentToPermissionAdministratorPermissionRequestState.class, permissionRequest.state().getClass());
    }

    @Test
    void receivedPermissionAdministratorResponse_throws() {
        // Given
        AiidaValidatedPermissionRequestState state = new AiidaValidatedPermissionRequestState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::receivedPermissionAdministratorResponse);
    }

    @Test
    void accept_throws() {
        // Given
        AiidaValidatedPermissionRequestState state = new AiidaValidatedPermissionRequestState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::accept);
    }

    @Test
    void invalid_throws() {
        // Given
        AiidaValidatedPermissionRequestState state = new AiidaValidatedPermissionRequestState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::invalid);
    }

    @Test
    void reject_throws() {
        // Given
        AiidaValidatedPermissionRequestState state = new AiidaValidatedPermissionRequestState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::reject);
    }

    @Test
    void terminate_throws() {
        // Given
        AiidaValidatedPermissionRequestState state = new AiidaValidatedPermissionRequestState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::terminate);
    }

    @Test
    void revoke_throws() {
        // Given
        AiidaValidatedPermissionRequestState state = new AiidaValidatedPermissionRequestState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::revoke);
    }

    @Test
    void timeLimit_throws() {
        // Given
        AiidaValidatedPermissionRequestState state = new AiidaValidatedPermissionRequestState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::fulfill);
    }

    @Test
    void timeOut_throws() {
        // Given
        AiidaValidatedPermissionRequestState state = new AiidaValidatedPermissionRequestState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::timeOut);
    }
}