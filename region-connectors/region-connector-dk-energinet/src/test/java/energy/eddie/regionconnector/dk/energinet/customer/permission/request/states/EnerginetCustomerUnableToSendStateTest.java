package energy.eddie.regionconnector.dk.energinet.customer.permission.request.states;

import energy.eddie.regionconnector.dk.energinet.customer.api.DkEnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.SimplePermissionRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnerginetCustomerUnableToSendStateTest {
    @Test
    void testToString() {
        // Given
        DkEnerginetCustomerPermissionRequest permissionRequest = new SimplePermissionRequest("pid", "cid");
        Throwable throwable = new Throwable("Sample error message");
        EnerginetCustomerUnableToSendState unableToSendState = new EnerginetCustomerUnableToSendState(permissionRequest, throwable);

        // When:
        String toStringResult = unableToSendState.toString();

        // Then
        String expectedToString = "UnableToSendState{t=java.lang.Throwable: Sample error message}";
        assertEquals(expectedToString, toStringResult);
    }
}
