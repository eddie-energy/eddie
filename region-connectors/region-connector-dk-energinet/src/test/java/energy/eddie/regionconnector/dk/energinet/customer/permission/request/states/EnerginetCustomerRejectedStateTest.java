package energy.eddie.regionconnector.dk.energinet.customer.permission.request.states;

import energy.eddie.regionconnector.dk.energinet.customer.permission.request.SimplePermissionRequest;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class EnerginetCustomerRejectedStateTest {
    @Test
    void constructor_doesNotThrow() {
        // Given
        DkEnerginetCustomerPermissionRequest permissionRequest = new SimplePermissionRequest("pid", "cid", "dataNeedId");

        // When
        // Then
        assertDoesNotThrow(() -> new EnerginetCustomerRejectedState(permissionRequest));
    }
}
