package energy.eddie.regionconnector.dk.energinet.customer.permission.request.states;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.process.model.PastStateException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnerginetCustomerMalformedStateTest {
    @Test
    void status_returnsMalformed() {
        // Given
        EnerginetCustomerMalformedState state = new EnerginetCustomerMalformedState(null, null);

        // When
        // Then
        assertEquals(PermissionProcessStatus.MALFORMED, state.status());
    }

    @Test
    void malformedStateToString_containsCause() {
        // Given
        var malformed = new EnerginetCustomerMalformedState(null, new Exception());

        // When
        var res = malformed.toString();

        // Then
        assertEquals("MalformedPermissionRequestState{cause=java.lang.Exception}", res);
    }

    @Test
    void validate_throws() {
        // Given
        EnerginetCustomerMalformedState state = new EnerginetCustomerMalformedState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::validate);
    }

    @Test
    void sendToPermissionAdministrator_throws() {
        // Given
        EnerginetCustomerMalformedState state = new EnerginetCustomerMalformedState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::sendToPermissionAdministrator);
    }

    @Test
    void receivedPermissionAdministratorResponse_throws() {
        // Given
        EnerginetCustomerMalformedState state = new EnerginetCustomerMalformedState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::receivedPermissionAdministratorResponse);
    }

    @Test
    void accept_throws() {
        // Given
        EnerginetCustomerMalformedState state = new EnerginetCustomerMalformedState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::accept);
    }

    @Test
    void invalid_throws() {
        // Given
        EnerginetCustomerMalformedState state = new EnerginetCustomerMalformedState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::invalid);
    }

    @Test
    void reject_throws() {
        // Given
        EnerginetCustomerMalformedState state = new EnerginetCustomerMalformedState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::reject);
    }

    @Test
    void terminate_throws() {
        // Given
        EnerginetCustomerMalformedState state = new EnerginetCustomerMalformedState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::terminate);
    }

    @Test
    void revoke_throws() {
        // Given
        EnerginetCustomerMalformedState state = new EnerginetCustomerMalformedState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::revoke);
    }

    @Test
    void timeLimit_throws() {
        // Given
        EnerginetCustomerMalformedState state = new EnerginetCustomerMalformedState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::fulfill);
    }

    @Test
    void timeOut_throws() {
        // Given
        EnerginetCustomerMalformedState state = new EnerginetCustomerMalformedState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::timeOut);
    }
}