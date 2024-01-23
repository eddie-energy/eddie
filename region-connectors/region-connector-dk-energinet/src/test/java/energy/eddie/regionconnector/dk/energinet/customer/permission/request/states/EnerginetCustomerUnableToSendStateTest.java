package energy.eddie.regionconnector.dk.energinet.customer.permission.request.states;

import energy.eddie.api.agnostic.process.model.PastStateException;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.SimplePermissionRequest;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnerginetCustomerUnableToSendStateTest {
    @Test
    void status_returnsUnableToSend() {
        // Given
        EnerginetCustomerUnableToSendState state = new EnerginetCustomerUnableToSendState(null, null);

        // When
        // Then
        assertEquals(PermissionProcessStatus.UNABLE_TO_SEND, state.status());
    }

    @Test
    void unableToSendStateToString_containsCause() {
        // Given
        DkEnerginetCustomerPermissionRequest permissionRequest = new SimplePermissionRequest("pid", "cid", "dataNeedId");
        Throwable throwable = new Throwable("Sample error message");
        EnerginetCustomerUnableToSendState unableToSendState = new EnerginetCustomerUnableToSendState(permissionRequest, throwable);

        // When:
        String toStringResult = unableToSendState.toString();

        // Then
        String expectedToString = "UnableToSendState{t=java.lang.Throwable: Sample error message}";
        assertEquals(expectedToString, toStringResult);
    }

    @Test
    void validate_throws() {
        // Given
        EnerginetCustomerUnableToSendState state = new EnerginetCustomerUnableToSendState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::validate);
    }

    @Test
    void sendToPermissionAdministrator_throws() {
        // Given
        EnerginetCustomerUnableToSendState state = new EnerginetCustomerUnableToSendState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::sendToPermissionAdministrator);
    }

    @Test
    void receivedPermissionAdministratorResponse_throws() {
        // Given
        EnerginetCustomerUnableToSendState state = new EnerginetCustomerUnableToSendState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::receivedPermissionAdministratorResponse);
    }

    @Test
    void accept_throws() {
        // Given
        EnerginetCustomerUnableToSendState state = new EnerginetCustomerUnableToSendState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::accept);
    }

    @Test
    void invalid_throws() {
        // Given
        EnerginetCustomerUnableToSendState state = new EnerginetCustomerUnableToSendState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::invalid);
    }

    @Test
    void reject_throws() {
        // Given
        EnerginetCustomerUnableToSendState state = new EnerginetCustomerUnableToSendState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::reject);
    }

    @Test
    void terminate_throws() {
        // Given
        EnerginetCustomerUnableToSendState state = new EnerginetCustomerUnableToSendState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::terminate);
    }

    @Test
    void revoke_throws() {
        // Given
        EnerginetCustomerUnableToSendState state = new EnerginetCustomerUnableToSendState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::revoke);
    }

    @Test
    void timeLimit_throws() {
        // Given
        EnerginetCustomerUnableToSendState state = new EnerginetCustomerUnableToSendState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::fulfill);
    }

    @Test
    void timeOut_throws() {
        // Given
        EnerginetCustomerUnableToSendState state = new EnerginetCustomerUnableToSendState(null, null);

        // When
        // Then
        assertThrows(PastStateException.class, state::timeOut);
    }
}