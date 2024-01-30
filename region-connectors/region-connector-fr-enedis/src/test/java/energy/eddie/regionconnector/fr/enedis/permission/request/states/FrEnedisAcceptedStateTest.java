package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.agnostic.process.model.states.TerminatedPermissionRequestState;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class FrEnedisAcceptedStateTest {

    @Test
    void terminate_transitionsToTerminated() {
        // Given
        var request = new EnedisPermissionRequest(
                "cid",
                "dnid",
                ZonedDateTime.now(ZoneOffset.UTC),
                ZonedDateTime.now(ZoneOffset.UTC)
        );
        FrEnedisAcceptedState acceptedState = new FrEnedisAcceptedState(request);

        // When
        acceptedState.terminate();

        // Then
        assertInstanceOf(TerminatedPermissionRequestState.class, request.state());
    }
}