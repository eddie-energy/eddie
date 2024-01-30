package energy.eddie.regionconnector.dk.energinet.permission.request.states;

import energy.eddie.api.agnostic.process.model.PastStateException;
import energy.eddie.api.v0.PermissionProcessStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnerginetCustomerRejectedStateTest {
    @Test
    void status_returnsRejected() {
        // Given
        EnerginetCustomerRejectedState state = new EnerginetCustomerRejectedState(null);

        // When
        // Then
        assertEquals(PermissionProcessStatus.REJECTED, state.status());
    }

    @Test
    void validate_throws() {
        // Given
        EnerginetCustomerRejectedState state = new EnerginetCustomerRejectedState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::validate);
    }

    @Test
    void sendToPermissionAdministrator_throws() {
        // Given
        EnerginetCustomerRejectedState state = new EnerginetCustomerRejectedState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::sendToPermissionAdministrator);
    }

    @Test
    void receivedPermissionAdministratorResponse_throws() {
        // Given
        EnerginetCustomerRejectedState state = new EnerginetCustomerRejectedState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::receivedPermissionAdministratorResponse);
    }

    @Test
    void accept_throws() {
        // Given
        EnerginetCustomerRejectedState state = new EnerginetCustomerRejectedState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::accept);
    }

    @Test
    void invalid_throws() {
        // Given
        EnerginetCustomerRejectedState state = new EnerginetCustomerRejectedState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::invalid);
    }

    @Test
    void reject_throws() {
        // Given
        EnerginetCustomerRejectedState state = new EnerginetCustomerRejectedState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::reject);
    }

    @Test
    void terminate_throws() {
        // Given
        EnerginetCustomerRejectedState state = new EnerginetCustomerRejectedState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::terminate);
    }

    @Test
    void revoke_throws() {
        // Given
        EnerginetCustomerRejectedState state = new EnerginetCustomerRejectedState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::revoke);
    }

    @Test
    void timeLimit_throws() {
        // Given
        EnerginetCustomerRejectedState state = new EnerginetCustomerRejectedState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::fulfill);
    }

    @Test
    void timeOut_throws() {
        // Given
        EnerginetCustomerRejectedState state = new EnerginetCustomerRejectedState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::timeOut);
    }
}