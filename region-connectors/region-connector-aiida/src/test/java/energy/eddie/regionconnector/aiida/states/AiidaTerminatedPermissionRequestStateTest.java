package energy.eddie.regionconnector.aiida.states;

import energy.eddie.api.agnostic.process.model.PastStateException;
import energy.eddie.api.v0.PermissionProcessStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiidaTerminatedPermissionRequestStateTest {
    @Test
    void status_returnsTerminated() {
        // Given
        AiidaTerminatedPermissionRequestState state = new AiidaTerminatedPermissionRequestState(null);

        // When
        // Then
        assertEquals(PermissionProcessStatus.TERMINATED, state.status());
    }

    @Test
    void validate_throws() {
        // Given
        AiidaTerminatedPermissionRequestState state = new AiidaTerminatedPermissionRequestState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::validate);
    }

    @Test
    void sendToPermissionAdministrator_throws() {
        // Given
        AiidaTerminatedPermissionRequestState state = new AiidaTerminatedPermissionRequestState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::sendToPermissionAdministrator);
    }

    @Test
    void receivedPermissionAdministratorResponse_throws() {
        // Given
        AiidaTerminatedPermissionRequestState state = new AiidaTerminatedPermissionRequestState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::receivedPermissionAdministratorResponse);
    }

    @Test
    void accept_throws() {
        // Given
        AiidaTerminatedPermissionRequestState state = new AiidaTerminatedPermissionRequestState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::accept);
    }

    @Test
    void invalid_throws() {
        // Given
        AiidaTerminatedPermissionRequestState state = new AiidaTerminatedPermissionRequestState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::invalid);
    }

    @Test
    void reject_throws() {
        // Given
        AiidaTerminatedPermissionRequestState state = new AiidaTerminatedPermissionRequestState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::reject);
    }

    @Test
    void terminate_throws() {
        // Given
        AiidaTerminatedPermissionRequestState state = new AiidaTerminatedPermissionRequestState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::terminate);
    }

    @Test
    void revoke_throws() {
        // Given
        AiidaTerminatedPermissionRequestState state = new AiidaTerminatedPermissionRequestState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::revoke);
    }

    @Test
    void timeLimit_throws() {
        // Given
        AiidaTerminatedPermissionRequestState state = new AiidaTerminatedPermissionRequestState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::fulfill);
    }

    @Test
    void timeOut_throws() {
        // Given
        AiidaTerminatedPermissionRequestState state = new AiidaTerminatedPermissionRequestState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::timeOut);
    }
}