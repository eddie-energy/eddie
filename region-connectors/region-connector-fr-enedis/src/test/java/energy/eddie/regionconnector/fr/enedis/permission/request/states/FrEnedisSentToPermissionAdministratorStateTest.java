package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.StateBuilderFactory;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FrEnedisSentToPermissionAdministratorStateTest {

    @Test
    void reject_transitionsStateToRejected() {
        // Given
        ZonedDateTime start = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime end = start.plusDays(1);
        StateBuilderFactory factory = new StateBuilderFactory();
        FrEnedisPermissionRequest permissionRequest = new EnedisPermissionRequest("pid", "cid", "dnid", start, end, Granularity.P1D, factory);
        FrEnedisSentToPermissionAdministratorState state = new FrEnedisSentToPermissionAdministratorState(permissionRequest, factory);

        // When
        state.reject();

        // Then
        assertEquals(FrEnedisRejectedState.class, permissionRequest.state().getClass());
    }

    @Test
    void invalid_transitionsStateToInvalid() {
        // Given
        ZonedDateTime start = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime end = start.plusDays(1);
        StateBuilderFactory factory = new StateBuilderFactory();
        FrEnedisPermissionRequest permissionRequest = new EnedisPermissionRequest("pid", "cid", "dnid", start, end, Granularity.P1D, factory);
        FrEnedisSentToPermissionAdministratorState state = new FrEnedisSentToPermissionAdministratorState(permissionRequest, factory);

        // When
        state.invalid();

        // Then
        assertEquals(FrEnedisInvalidState.class, permissionRequest.state().getClass());
    }

    @Test
    void accept_transitionsStateToAccepted() {
        // Given
        ZonedDateTime start = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime end = start.plusDays(1);
        StateBuilderFactory factory = new StateBuilderFactory();
        FrEnedisPermissionRequest permissionRequest = new EnedisPermissionRequest("pid", "cid", "dnid", start, end, Granularity.P1D, factory);
        FrEnedisSentToPermissionAdministratorState state = new FrEnedisSentToPermissionAdministratorState(permissionRequest, factory);

        // When
        state.accept();

        // Then
        assertEquals(FrEnedisAcceptedState.class, permissionRequest.state().getClass());
    }
}