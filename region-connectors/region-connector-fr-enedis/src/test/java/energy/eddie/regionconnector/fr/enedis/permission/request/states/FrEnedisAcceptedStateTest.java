package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.states.FulfilledPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.RevokedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.states.TerminatedPermissionRequestState;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.StateBuilderFactory;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class FrEnedisAcceptedStateTest {

    @Test
    void terminate_transitionsToTerminated() {
        // Given
        StateBuilderFactory factory = new StateBuilderFactory();
        var request = new EnedisPermissionRequest(
                "cid",
                "dnid",
                ZonedDateTime.now(ZoneOffset.UTC),
                ZonedDateTime.now(ZoneOffset.UTC),
                Granularity.P1D,
                factory);
        FrEnedisAcceptedState acceptedState = new FrEnedisAcceptedState(request, factory);

        // When
        acceptedState.terminate();

        // Then
        assertInstanceOf(TerminatedPermissionRequestState.class, request.state());
    }

    @Test
    void fulfill_transitionsToFulfilled() {
        // Given
        StateBuilderFactory factory = new StateBuilderFactory();
        var request = new EnedisPermissionRequest(
                "cid",
                "dnid",
                ZonedDateTime.now(ZoneOffset.UTC),
                ZonedDateTime.now(ZoneOffset.UTC),
                Granularity.P1D,
                factory);
        FrEnedisAcceptedState acceptedState = new FrEnedisAcceptedState(request, factory);

        // When
        acceptedState.fulfill();

        // Then
        assertInstanceOf(FulfilledPermissionRequestState.class, request.state());
    }

    @Test
    void revoke_transitionsToRevoked() {
        // Given
        StateBuilderFactory factory = new StateBuilderFactory();
        var request = new EnedisPermissionRequest(
                "cid",
                "dnid",
                ZonedDateTime.now(ZoneOffset.UTC),
                ZonedDateTime.now(ZoneOffset.UTC),
                Granularity.P1D,
                factory);
        FrEnedisAcceptedState acceptedState = new FrEnedisAcceptedState(request, factory);

        // When
        acceptedState.revoke();

        // Then
        assertInstanceOf(RevokedPermissionRequestState.class, request.state());
    }
}
