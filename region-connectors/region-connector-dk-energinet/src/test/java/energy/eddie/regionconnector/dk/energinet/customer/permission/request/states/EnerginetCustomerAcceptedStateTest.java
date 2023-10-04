package energy.eddie.regionconnector.dk.energinet.customer.permission.request.states;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class EnerginetCustomerAcceptedStateTest {
    @Test
    void terminateNotImplemented() {
        // Given
        EnerginetCustomerAcceptedState acceptedState = new EnerginetCustomerAcceptedState(null);

        // When
        // Then
        assertThrows(UnsupportedOperationException.class, acceptedState::terminate);
    }
}
