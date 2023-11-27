package energy.eddie.regionconnector.at.eda.permission.request.states;

import energy.eddie.api.v0.process.model.PastStateException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MalformedPermissionRequestStateTest {

    @Test
    void malformedStateToString_containsCause() {
        // Given
        var malformed = new AtMalformedPermissionRequestState(null, new Exception());

        // When
        var res = malformed.toString();

        // Then
        assertEquals("MalformedPermissionRequestState{cause=java.lang.Exception}", res);
    }

    @Test
    void validate_throws() {
        // Given
        AtMalformedPermissionRequestState state = new AtMalformedPermissionRequestState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::validate);
    }

    @Test
    void sendToPermissionAdministrator_throws() {
        // Given
        AtMalformedPermissionRequestState state = new AtMalformedPermissionRequestState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::sendToPermissionAdministrator);
    }

    @Test
    void receivedPermissionAdministratorResponse_throws() {
        // Given
        AtMalformedPermissionRequestState state = new AtMalformedPermissionRequestState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::receivedPermissionAdministratorResponse);
    }

    @Test
    void accept_throws() {
        // Given
        AtMalformedPermissionRequestState state = new AtMalformedPermissionRequestState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::accept);
    }

    @Test
    void invalid_throws() {
        // Given
        AtMalformedPermissionRequestState state = new AtMalformedPermissionRequestState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::invalid);
    }

    @Test
    void reject_throws() {
        // Given
        AtMalformedPermissionRequestState state = new AtMalformedPermissionRequestState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::reject);
    }

    @Test
    void terminate_throws() {
        // Given
        AtMalformedPermissionRequestState state = new AtMalformedPermissionRequestState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::terminate);
    }
}