package energy.eddie.regionconnector.at.eda.permission.request.states;

import energy.eddie.regionconnector.at.api.PastStateException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UnableToSendPermissionRequestStateTest {
    @Test
    void unableToSendPermissionRequestStateToString_containsCause() {
        // Given
        var malformed = new UnableToSendPermissionRequestState(null, new Exception());

        // When
        var res = malformed.toString();

        // Then
        assertEquals("UnableToSendPermissionRequestState{cause=java.lang.Exception}", res);
    }

    @Test
    void validate_throws() {
        // Given
        UnableToSendPermissionRequestState state = new UnableToSendPermissionRequestState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::validate);
    }

    @Test
    void sendToPermissionAdministrator_throws() {
        // Given
        UnableToSendPermissionRequestState state = new UnableToSendPermissionRequestState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::sendToPermissionAdministrator);
    }

    @Test
    void receivedPermissionAdministratorResponse_throws() {
        // Given
        UnableToSendPermissionRequestState state = new UnableToSendPermissionRequestState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::receivedPermissionAdministratorResponse);
    }

    @Test
    void accept_throws() {
        // Given
        UnableToSendPermissionRequestState state = new UnableToSendPermissionRequestState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::accept);
    }

    @Test
    void invalid_throws() {
        // Given
        UnableToSendPermissionRequestState state = new UnableToSendPermissionRequestState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::invalid);
    }

    @Test
    void reject_throws() {
        // Given
        UnableToSendPermissionRequestState state = new UnableToSendPermissionRequestState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::reject);
    }

    @Test
    void terminate_throws() {
        // Given
        UnableToSendPermissionRequestState state = new UnableToSendPermissionRequestState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::terminate);
    }
}