package energy.eddie.regionconnector.shared.timeout;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TimeoutConfigurationTest {
    @Test
    void testTimeoutConfiguration_constructs_withValidParameters() {
        // Given
        var duration = 24;

        // When
        var res = new TimeoutConfiguration(duration);

        // Then
        assertEquals(duration, res.duration());
    }

    @ParameterizedTest
    @ValueSource(ints = {-10, 0})
    void testTimeoutConfiguration_throws_withInvalidParameters(int duration) {
        // Given
        // When & Then
        assertThrows(InvalidTimeoutConfigurationException.class, () -> new TimeoutConfiguration(duration));
    }
}