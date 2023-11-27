package energy.eddie.regionconnector.aiida.states;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.process.model.PastStateException;
import energy.eddie.regionconnector.aiida.api.AiidaPermissionRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AiidaAcceptedPermissionRequestStateTest {
    @Test
    void status_returnsAccepted() {
        // Given
        AiidaAcceptedPermissionRequestState state = new AiidaAcceptedPermissionRequestState(null);

        // When
        // Then
        assertEquals(PermissionProcessStatus.ACCEPTED, state.status());
    }

    @Test
    void terminate_changesState() {
        // Given
        var now = Instant.now();
        AiidaPermissionRequest request = new AiidaPermissionRequest("foo", "bar", "loo", "too", now, now, null);
        AiidaAcceptedPermissionRequestState state = new AiidaAcceptedPermissionRequestState(request);

        // When
        assertDoesNotThrow(state::terminate);

        // Then
        assertEquals(AiidaTerminatedPermissionRequestState.class, request.state().getClass());
    }

    @Test
    void revoke_changesState() {
        // Given
        var now = Instant.now();
        AiidaPermissionRequest request = new AiidaPermissionRequest("foo", "bar", "loo", "too", now, now, null);
        AiidaAcceptedPermissionRequestState state = new AiidaAcceptedPermissionRequestState(request);

        // When
        assertDoesNotThrow(state::revoke);

        // Then
        assertEquals(AiidaRevokedPermissionRequestState.class, request.state().getClass());
    }

    @Test
    void timeLimit_changesState() {
        // Given
        var now = Instant.now();
        AiidaPermissionRequest request = new AiidaPermissionRequest("foo", "bar", "loo", "too", now, now, null);
        AiidaAcceptedPermissionRequestState state = new AiidaAcceptedPermissionRequestState(request);

        // When
        assertDoesNotThrow(state::timeLimit);

        // Then
        assertEquals(AiidaTimeLimitPermissionRequestState.class, request.state().getClass());
    }

    @Test
    void validate_throws() {
        // Given
        AiidaAcceptedPermissionRequestState state = new AiidaAcceptedPermissionRequestState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::validate);
    }

    @Test
    void sendToPermissionAdministrator_throws() {
        // Given
        AiidaAcceptedPermissionRequestState state = new AiidaAcceptedPermissionRequestState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::sendToPermissionAdministrator);
    }

    @Test
    void receivedPermissionAdministratorResponse_throws() {
        // Given
        AiidaAcceptedPermissionRequestState state = new AiidaAcceptedPermissionRequestState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::receivedPermissionAdministratorResponse);
    }

    @Test
    void accept_throws() {
        // Given
        AiidaAcceptedPermissionRequestState state = new AiidaAcceptedPermissionRequestState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::accept);
    }

    @Test
    void invalid_throws() {
        // Given
        AiidaAcceptedPermissionRequestState state = new AiidaAcceptedPermissionRequestState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::invalid);
    }

    @Test
    void reject_throws() {
        // Given
        AiidaAcceptedPermissionRequestState state = new AiidaAcceptedPermissionRequestState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::reject);
    }

    @Test
    void timeOut_throws() {
        // Given
        AiidaAcceptedPermissionRequestState state = new AiidaAcceptedPermissionRequestState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::timeOut);
    }
}