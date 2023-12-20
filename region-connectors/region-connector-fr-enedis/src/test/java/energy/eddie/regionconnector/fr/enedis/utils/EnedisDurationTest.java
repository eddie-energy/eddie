package energy.eddie.regionconnector.fr.enedis.utils;

import energy.eddie.api.v0.process.model.TimeframedPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnedisDurationTest {

    private static Stream<Arguments> testEnedisDuration_returnsISODuration_ifEndIsInFuture_methodSource() {
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC);
        return Stream.of(
                Arguments.of(start, start.plusDays(3), "P2D"),
                Arguments.of(start, start.plusYears(3), "P1095D")
        );
    }

    @Test
    void testEnedisDuration_returnsISODurationOfOneDay_ifEndIsInPast() {
        // Given
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).minusDays(10);
        ZonedDateTime end = start.plusDays(3);
        TimeframedPermissionRequest permissionRequest = new EnedisPermissionRequest("cid", "dnid", start, end);
        EnedisDuration duration = new EnedisDuration(permissionRequest);

        // When
        var res = duration.toString();

        // Then
        assertEquals("P1D", res);
    }

    @ParameterizedTest
    @MethodSource("testEnedisDuration_returnsISODuration_ifEndIsInFuture_methodSource")
    void testEnedisDuration_returnsISODuration_ifEndIsInFuture(ZonedDateTime start, ZonedDateTime end, String expected) {
        // Given
        TimeframedPermissionRequest permissionRequest = new EnedisPermissionRequest("cid", "dnid", start, end);
        EnedisDuration duration = new EnedisDuration(permissionRequest);

        // When
        var res = duration.toString();

        // Then
        assertEquals(expected, res);
    }
}