package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.regionconnector.fr.enedis.permission.request.SimplePermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FrEnedisUnableToSendStateTest {
    @Test
    void testToString() {
        // Given
        FrEnedisPermissionRequest permissionRequest = new SimplePermissionRequest("pid", "cid");
        Throwable throwable = new Throwable("Sample error message");
        FrEnedisUnableToSendState unableToSendState = new FrEnedisUnableToSendState(permissionRequest, throwable);

        // When:
        String toStringResult = unableToSendState.toString();

        // Then
        String expectedToString = "UnableToSendState{t=java.lang.Throwable: Sample error message}";
        assertEquals(expectedToString, toStringResult);
    }
}