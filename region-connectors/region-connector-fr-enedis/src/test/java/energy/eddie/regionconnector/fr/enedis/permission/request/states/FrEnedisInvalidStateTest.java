package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.SimplePermissionRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class FrEnedisInvalidStateTest {

    @Test
    void invalidState_doesNotThrow() {
        // Given
        TimeframedPermissionRequest timeframedPermissionRequest = new SimplePermissionRequest("pid", "cid");
        // When
        // Then
        assertDoesNotThrow(() -> new FrEnedisInvalidState(timeframedPermissionRequest));
    }
}