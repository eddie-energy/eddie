package energy.eddie.regionconnector.dk.energinet.customer.permission.request.states;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.process.model.PastStateException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnerginetCustomerAcceptedStateTest {
    @Test
    void status_returnsAccepted() {
        // Given
        EnerginetCustomerAcceptedState state = new EnerginetCustomerAcceptedState(null);

        // When
        // Then
        assertEquals(PermissionProcessStatus.ACCEPTED, state.status());
    }

    @Test
    void terminate_notImplemented() {
        // Given
        EnerginetCustomerAcceptedState state = new EnerginetCustomerAcceptedState(null);

        // When
        // Then
        assertThrows(UnsupportedOperationException.class, state::terminate);
    }

    @Test
    void revoke_notImplemented() {
        // Given
        EnerginetCustomerAcceptedState state = new EnerginetCustomerAcceptedState(null);

        // When
        // Then
        assertThrows(UnsupportedOperationException.class, state::revoke);
    }

    @Test
    void timeLimit_notImplemented() {
        // Given
        EnerginetCustomerAcceptedState state = new EnerginetCustomerAcceptedState(null);

        // When
        // Then
        assertThrows(UnsupportedOperationException.class, state::timeLimit);
    }

    @Test
    void validate_throws() {
        // Given
        EnerginetCustomerAcceptedState state = new EnerginetCustomerAcceptedState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::validate);
    }

    @Test
    void sendToPermissionAdministrator_throws() {
        // Given
        EnerginetCustomerAcceptedState state = new EnerginetCustomerAcceptedState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::sendToPermissionAdministrator);
    }

    @Test
    void receivedPermissionAdministratorResponse_throws() {
        // Given
        EnerginetCustomerAcceptedState state = new EnerginetCustomerAcceptedState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::receivedPermissionAdministratorResponse);
    }

    @Test
    void accept_throws() {
        // Given
        EnerginetCustomerAcceptedState state = new EnerginetCustomerAcceptedState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::accept);
    }

    @Test
    void invalid_throws() {
        // Given
        EnerginetCustomerAcceptedState state = new EnerginetCustomerAcceptedState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::invalid);
    }

    @Test
    void reject_throws() {
        // Given
        EnerginetCustomerAcceptedState state = new EnerginetCustomerAcceptedState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::reject);
    }

    @Test
    void timeOut_throws() {
        // Given
        EnerginetCustomerAcceptedState state = new EnerginetCustomerAcceptedState(null);

        // When
        // Then
        assertThrows(PastStateException.class, state::timeOut);
    }
}
