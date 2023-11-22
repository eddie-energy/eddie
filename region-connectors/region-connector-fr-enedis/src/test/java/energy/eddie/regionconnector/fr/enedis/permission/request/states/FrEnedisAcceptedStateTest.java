package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class FrEnedisAcceptedStateTest {

    @Test
    void terminateNotImplemented() {
        // Given
        FrEnedisAcceptedState acceptedState = new FrEnedisAcceptedState(null);

        // When
        // Then
        assertThrows(UnsupportedOperationException.class, acceptedState::terminate);
    }
}