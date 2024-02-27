package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest;
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
        FrEnedisPermissionRequest permissionRequest = new EnedisPermissionRequest("pid", "cid", "dnid", start, end);
        FrEnedisSentToPermissionAdministratorState state = new FrEnedisSentToPermissionAdministratorState(permissionRequest);

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
        FrEnedisPermissionRequest permissionRequest = new EnedisPermissionRequest("pid", "cid", "dnid", start, end);
        FrEnedisSentToPermissionAdministratorState state = new FrEnedisSentToPermissionAdministratorState(permissionRequest);

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
        FrEnedisPermissionRequest permissionRequest = new EnedisPermissionRequest("pid", "cid", "dnid", start, end);
        FrEnedisSentToPermissionAdministratorState state = new FrEnedisSentToPermissionAdministratorState(permissionRequest);

        // When
        state.accept();

        // Then
        assertEquals(FrEnedisAcceptedState.class, permissionRequest.state().getClass());
    }
}