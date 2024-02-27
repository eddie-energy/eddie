package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FrEnedisPendingAcknowledgmentStateTest {

    @Test
    void receivedPermissionAdminAnswer_transitionsState() {
        // Given
        ZonedDateTime start = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime end = start.plusDays(1);
        FrEnedisPermissionRequest permissionRequest = new EnedisPermissionRequest("pid", "cid", "dnid", start, end);
        FrEnedisPendingAcknowledgmentState state = new FrEnedisPendingAcknowledgmentState(permissionRequest);

        // When
        state.receivedPermissionAdministratorResponse();

        // Then
        assertEquals(FrEnedisSentToPermissionAdministratorState.class, permissionRequest.state().getClass());
    }
}