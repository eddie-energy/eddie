package energy.eddie.regionconnector.dk.energinet.customer.permission.request.states;

import energy.eddie.regionconnector.dk.energinet.customer.api.DkEnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.SimplePermissionRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class EnerginetCustomerInvalidStateTest {
    @Test
    void invalidState_doesNotThrow() {
        // Given
        DkEnerginetCustomerPermissionRequest timeframedPermissionRequest = new SimplePermissionRequest("pid", "cid", "dataNeedId");
        // When
        // Then
        assertDoesNotThrow(() -> new EnerginetCustomerInvalidState(timeframedPermissionRequest));
    }
}
