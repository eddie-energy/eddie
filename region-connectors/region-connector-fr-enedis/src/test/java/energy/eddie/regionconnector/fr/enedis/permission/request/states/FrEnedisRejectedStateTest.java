package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.regionconnector.fr.enedis.permission.request.SimplePermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class FrEnedisRejectedStateTest {

    @Test
    void constructor_doesNotThrow() {
        // Given
        FrEnedisPermissionRequest permissionRequest = new SimplePermissionRequest("pid", "cid");

        // When
        // Then
        assertDoesNotThrow(() -> new FrEnedisRejectedState(permissionRequest));
    }
}