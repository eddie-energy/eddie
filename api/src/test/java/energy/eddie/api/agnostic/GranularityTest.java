package energy.eddie.api.agnostic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GranularityTest {
    @Test
    void testFromMinutes_withValidMinutes() {
        // Given
        var minutes = 60;

        // When
        var res = Granularity.fromMinutes(minutes);

        // Then
        assertEquals(Granularity.PT1H, res);
    }

    @Test
    void testFromMinutes_withInvalidMinutes() {
        // Given
        var minutes = 38;

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> Granularity.fromMinutes(minutes));
    }
}