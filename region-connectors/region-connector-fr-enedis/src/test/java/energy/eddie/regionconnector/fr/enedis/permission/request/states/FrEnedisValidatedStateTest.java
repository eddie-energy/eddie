package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.v0.process.model.TimeframedPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FrEnedisValidatedStateTest {
    @Test
    void sendToPermissionAdministrator_transitionsStatePendingAcknowledgmentState() {
        // Given
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        TimeframedPermissionRequest permissionRequest = new EnedisPermissionRequest("pid", "cid", "dnid", now, now.plusDays(1));
        FrEnedisValidatedState state = new FrEnedisValidatedState(permissionRequest);

        // When
        state.sendToPermissionAdministrator();

        // Then
        assertEquals(FrEnedisPendingAcknowledgmentState.class, permissionRequest.state().getClass());
    }
}