package energy.eddie.regionconnector.fr.enedis.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnedisDurationTest {

    private static Stream<Arguments> testEnedisDuration_returnsISODuration_ifEndIsInFuture_methodSource() {
        LocalDate now = LocalDate.now(ZoneOffset.UTC);
        return Stream.of(
                Arguments.of(now.plusDays(3), "P3D"),
                Arguments.of(now.plusYears(3), "P1095D")
        );
    }

    @Test
    void testEnedisDuration_returnsISODurationOfOneDay_ifEndIsInPast() {
        // Given
        LocalDate end = LocalDate.now(ZoneOffset.UTC).minusDays(10);
        EnedisDuration duration = new EnedisDuration(end);

        // When
        var res = duration.toString();

        // Then
        assertEquals("P1D", res);
    }

    @ParameterizedTest
    @MethodSource("testEnedisDuration_returnsISODuration_ifEndIsInFuture_methodSource")
    void testEnedisDuration_returnsISODuration_ifEndIsInFuture(LocalDate end, String expected) {
        // Given
        EnedisDuration duration = new EnedisDuration(end);

        // When
        var res = duration.toString();

        // Then
        assertEquals(expected, res);
    }
}
