package energy.eddie.regionconnector.dk.energinet.customer.permission.request.states;

import energy.eddie.regionconnector.dk.energinet.customer.api.DkEnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.SimplePermissionRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class EnerginetCustomerRejectedStateTest {
    @Test
    void constructor_doesNotThrow() {
        // Given
        DkEnerginetCustomerPermissionRequest permissionRequest = new SimplePermissionRequest("pid", "cid");

        // When
        // Then
        assertDoesNotThrow(() -> new EnerginetCustomerRejectedState(permissionRequest));
    }
}
