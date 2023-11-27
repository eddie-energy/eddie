package energy.eddie.regionconnector.aiida.states;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.process.model.PastStateException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiidaTimeLimitPermissionRequestStateTest {
    @Test
    void status_returnsTimeLimit() {
        // Given
        AiidaTimeLimitPermissionRequestState state = new AiidaTimeLimitPermissionRequestState(null);

        // When
        // Then
        assertEquals(PermissionProcessStatus.TIME_LIMIT, state.status());
    }

    @Test
    void validate_throws() {
        // Given
        AiidaTimeLimitPermissionRequestState state = new AiidaTimeLimitPermissionRequestState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::validate);
    }

    @Test
    void sendToPermissionAdministrator_throws() {
        // Given
        AiidaTimeLimitPermissionRequestState state = new AiidaTimeLimitPermissionRequestState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::sendToPermissionAdministrator);
    }

    @Test
    void receivedPermissionAdministratorResponse_throws() {
        // Given
        AiidaTimeLimitPermissionRequestState state = new AiidaTimeLimitPermissionRequestState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::receivedPermissionAdministratorResponse);
    }

    @Test
    void accept_throws() {
        // Given
        AiidaTimeLimitPermissionRequestState state = new AiidaTimeLimitPermissionRequestState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::accept);
    }

    @Test
    void invalid_throws() {
        // Given
        AiidaTimeLimitPermissionRequestState state = new AiidaTimeLimitPermissionRequestState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::invalid);
    }

    @Test
    void reject_throws() {
        // Given
        AiidaTimeLimitPermissionRequestState state = new AiidaTimeLimitPermissionRequestState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::reject);
    }

    @Test
    void terminate_throws() {
        // Given
        AiidaTimeLimitPermissionRequestState state = new AiidaTimeLimitPermissionRequestState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::terminate);
    }

    @Test
    void revoke_throws() {
        // Given
        AiidaTimeLimitPermissionRequestState state = new AiidaTimeLimitPermissionRequestState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::revoke);
    }

    @Test
    void timeLimit_throws() {
        // Given
        AiidaTimeLimitPermissionRequestState state = new AiidaTimeLimitPermissionRequestState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::timeLimit);
    }

    @Test
    void timeOut_throws() {
        // Given
        AiidaTimeLimitPermissionRequestState state = new AiidaTimeLimitPermissionRequestState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::timeOut);
    }
}