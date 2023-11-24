package energy.eddie.regionconnector.dk.energinet.customer.permission.request.states;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.process.model.PastStateException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnerginetCustomerInvalidStateTest {
    @Test
    void status_returnsInvalid() {
        // Given
        EnerginetCustomerInvalidState state = new EnerginetCustomerInvalidState(null);

        // When
        // Then
        assertEquals(PermissionProcessStatus.INVALID, state.status());
    }

    @Test
    void validate_throws() {
        // Given
        EnerginetCustomerInvalidState state = new EnerginetCustomerInvalidState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::validate);
    }

    @Test
    void sendToPermissionAdministrator_throws() {
        // Given
        EnerginetCustomerInvalidState state = new EnerginetCustomerInvalidState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::sendToPermissionAdministrator);
    }

    @Test
    void receivedPermissionAdministratorResponse_throws() {
        // Given
        EnerginetCustomerInvalidState state = new EnerginetCustomerInvalidState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::receivedPermissionAdministratorResponse);
    }

    @Test
    void accept_throws() {
        // Given
        EnerginetCustomerInvalidState state = new EnerginetCustomerInvalidState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::accept);
    }

    @Test
    void invalid_throws() {
        // Given
        EnerginetCustomerInvalidState state = new EnerginetCustomerInvalidState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::invalid);
    }

    @Test
    void reject_throws() {
        // Given
        EnerginetCustomerInvalidState state = new EnerginetCustomerInvalidState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::reject);
    }

    @Test
    void terminate_throws() {
        // Given
        EnerginetCustomerInvalidState state = new EnerginetCustomerInvalidState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::terminate);
    }

    @Test
    void revoke_throws() {
        // Given
        EnerginetCustomerInvalidState state = new EnerginetCustomerInvalidState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::revoke);
    }

    @Test
    void timeLimit_throws() {
        // Given
        EnerginetCustomerInvalidState state = new EnerginetCustomerInvalidState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::timeLimit);
    }

    @Test
    void timeOut_throws() {
        // Given
        EnerginetCustomerInvalidState state = new EnerginetCustomerInvalidState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::timeOut);
    }
}
