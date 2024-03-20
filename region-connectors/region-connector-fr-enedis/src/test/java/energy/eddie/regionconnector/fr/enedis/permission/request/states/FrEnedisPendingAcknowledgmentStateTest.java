package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.StateBuilderFactory;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FrEnedisPendingAcknowledgmentStateTest {

    @Test
    void receivedPermissionAdminAnswer_transitionsState() {
        // Given
        LocalDate start = LocalDate.now(ZoneId.systemDefault());
        LocalDate end = start.plusDays(1);
        StateBuilderFactory factory = new StateBuilderFactory();
        FrEnedisPermissionRequest permissionRequest = new EnedisPermissionRequest("pid", "cid", "dnid", start, end, Granularity.P1D, factory);
        FrEnedisPendingAcknowledgmentState state = new FrEnedisPendingAcknowledgmentState(permissionRequest, factory);

        // When
        state.receivedPermissionAdministratorResponse();

        // Then
        assertEquals(FrEnedisSentToPermissionAdministratorState.class, permissionRequest.state().getClass());
    }
}
