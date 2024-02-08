package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.agnostic.process.model.PastStateException;
import energy.eddie.api.v0.PermissionProcessStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FrEnedisTerminatedStateTest {
    @Test
    void status_returnsTerminated() {
        // Given
        FrEnedisTerminatedState state = new FrEnedisTerminatedState(null);

        // When
        // Then
        assertEquals(PermissionProcessStatus.TERMINATED, state.status());
    }

    @Test
    void validate_throws() {
        // Given
        FrEnedisTerminatedState state = new FrEnedisTerminatedState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::validate);
    }

    @Test
    void sendToPermissionAdministrator_throws() {
        // Given
        FrEnedisTerminatedState state = new FrEnedisTerminatedState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::sendToPermissionAdministrator);
    }

    @Test
    void receivedPermissionAdministratorResponse_throws() {
        // Given
        FrEnedisTerminatedState state = new FrEnedisTerminatedState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::receivedPermissionAdministratorResponse);
    }

    @Test
    void accept_throws() {
        // Given
        FrEnedisTerminatedState state = new FrEnedisTerminatedState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::accept);
    }

    @Test
    void invalid_throws() {
        // Given
        FrEnedisTerminatedState state = new FrEnedisTerminatedState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::invalid);
    }

    @Test
    void reject_throws() {
        // Given
        FrEnedisTerminatedState state = new FrEnedisTerminatedState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::reject);
    }

    @Test
    void fulfill_throws() {
        // Given
        FrEnedisTerminatedState state = new FrEnedisTerminatedState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::fulfill);
    }

    @Test
    void revoke_throws() {
        // Given
        FrEnedisTerminatedState state = new FrEnedisTerminatedState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::revoke);
    }

    @Test
    void timeLimit_throws() {
        // Given
        FrEnedisTerminatedState state = new FrEnedisTerminatedState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::fulfill);
    }

    @Test
    void timeOut_throws() {
        // Given
        FrEnedisTerminatedState state = new FrEnedisTerminatedState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::timeOut);
    }
}
