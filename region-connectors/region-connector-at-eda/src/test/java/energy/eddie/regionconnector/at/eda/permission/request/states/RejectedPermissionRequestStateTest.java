package energy.eddie.regionconnector.at.eda.permission.request.states;

import energy.eddie.api.agnostic.process.model.PastStateException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class RejectedPermissionRequestStateTest {
    @Test
    void validate_throws() {
        // Given
        AtRejectedPermissionRequestState state = new AtRejectedPermissionRequestState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::validate);
    }

    @Test
    void sendToPermissionAdministrator_throws() {
        // Given
        AtRejectedPermissionRequestState state = new AtRejectedPermissionRequestState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::sendToPermissionAdministrator);
    }

    @Test
    void receivedPermissionAdministratorResponse_throws() {
        // Given
        AtRejectedPermissionRequestState state = new AtRejectedPermissionRequestState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::receivedPermissionAdministratorResponse);
    }

    @Test
    void accept_throws() {
        // Given
        AtRejectedPermissionRequestState state = new AtRejectedPermissionRequestState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::accept);
    }

    @Test
    void invalid_throws() {
        // Given
        AtRejectedPermissionRequestState state = new AtRejectedPermissionRequestState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::invalid);
    }

    @Test
    void reject_throws() {
        // Given
        AtRejectedPermissionRequestState state = new AtRejectedPermissionRequestState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::reject);
    }

    @Test
    void terminate_throws() {
        // Given
        AtRejectedPermissionRequestState state = new AtRejectedPermissionRequestState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::terminate);
    }
}