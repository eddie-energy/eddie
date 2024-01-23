package energy.eddie.regionconnector.aiida.states;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.process.model.FutureStateException;
import energy.eddie.regionconnector.aiida.permission.request.AiidaPermissionRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AiidaCreatedPermissionRequestStateTest {
    @Test
    void validate_changesToValidatedState_whenValid() {
        var now = Instant.now();
        AiidaPermissionRequest request = new AiidaPermissionRequest("foo", "bar",
                "loo", "too", now, now);

        // When
        assertDoesNotThrow(request::validate);

        // Then
        assertEquals(AiidaValidatedPermissionRequestState.class, request.state().getClass());
    }

    @Test
    void status_returnsCreated() {
        // Given
        AiidaCreatedPermissionRequestState state = new AiidaCreatedPermissionRequestState(null);

        // When
        // Then
        assertEquals(PermissionProcessStatus.CREATED, state.status());
    }

    @Test
    void sendToPermissionAdministrator_throws() {
        // Given
        AiidaCreatedPermissionRequestState state = new AiidaCreatedPermissionRequestState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::sendToPermissionAdministrator);
    }

    @Test
    void receivedPermissionAdministratorResponse_throws() {
        // Given
        AiidaCreatedPermissionRequestState state = new AiidaCreatedPermissionRequestState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::receivedPermissionAdministratorResponse);
    }

    @Test
    void accept_throws() {
        // Given
        AiidaCreatedPermissionRequestState state = new AiidaCreatedPermissionRequestState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::accept);
    }

    @Test
    void invalid_throws() {
        // Given
        AiidaCreatedPermissionRequestState state = new AiidaCreatedPermissionRequestState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::invalid);
    }

    @Test
    void reject_throws() {
        // Given
        AiidaCreatedPermissionRequestState state = new AiidaCreatedPermissionRequestState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::reject);
    }

    @Test
    void terminate_throws() {
        // Given
        AiidaCreatedPermissionRequestState state = new AiidaCreatedPermissionRequestState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::terminate);
    }

    @Test
    void revoke_throws() {
        // Given
        AiidaCreatedPermissionRequestState state = new AiidaCreatedPermissionRequestState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::revoke);
    }

    @Test
    void timeLimit_throws() {
        // Given
        AiidaCreatedPermissionRequestState state = new AiidaCreatedPermissionRequestState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::fulfill);
    }

    @Test
    void timeOut_throws() {
        // Given
        AiidaCreatedPermissionRequestState state = new AiidaCreatedPermissionRequestState(null);

        // When
        // Then
        assertThrows(FutureStateException.class, state::timeOut);
    }
}