package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.StateBuilderFactory;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FrEnedisValidatedStateTest {
    @Test
    void sendToPermissionAdministrator_transitionsStatePendingAcknowledgmentState() {
        // Given
        LocalDate now = LocalDate.now(ZoneId.systemDefault());
        StateBuilderFactory factory = new StateBuilderFactory();
        FrEnedisPermissionRequest permissionRequest = new EnedisPermissionRequest("pid", "cid", "dnid", now, now.plusDays(1), Granularity.P1D, factory);
        FrEnedisValidatedState state = new FrEnedisValidatedState(permissionRequest, factory);

        // When
        state.sendToPermissionAdministrator();

        // Then
        assertEquals(FrEnedisPendingAcknowledgmentState.class, permissionRequest.state().getClass());
    }
}
