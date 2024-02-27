package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FrEnedisValidatedStateTest {
    @Test
    void sendToPermissionAdministrator_transitionsStatePendingAcknowledgmentState() {
        // Given
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        FrEnedisPermissionRequest permissionRequest = new EnedisPermissionRequest("pid", "cid", "dnid", now, now.plusDays(1));
        FrEnedisValidatedState state = new FrEnedisValidatedState(permissionRequest);

        // When
        state.sendToPermissionAdministrator();

        // Then
        assertEquals(FrEnedisPendingAcknowledgmentState.class, permissionRequest.state().getClass());
    }
}