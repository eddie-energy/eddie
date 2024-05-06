package energy.eddie.regionconnector.es.datadis.permission.request;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DatadisPermissionRequestTest {

    public static Stream<Arguments> testMeasurementType_returnsExpectedValues() {
        return Stream.of(
                Arguments.of(Granularity.PT1H, MeasurementType.HOURLY),
                Arguments.of(Granularity.PT15M, MeasurementType.QUARTER_HOURLY)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testMeasurementType_returnsExpectedValues(Granularity granularity, MeasurementType measurementType) {
        // Given
        var pr = new DatadisPermissionRequest(
                "pid",
                "cid",
                "dnid",
                granularity,
                "NIF",
                "mpid",
                LocalDate.now(ZoneOffset.UTC),
                LocalDate.now(ZoneOffset.UTC),
                null,
                null,
                null,
                PermissionProcessStatus.CREATED,
                null,
                false,
                ZonedDateTime.now(ZoneOffset.UTC)
        );

        // When
        var res = pr.measurementType();

        // Then
        assertEquals(measurementType, res);
    }

    @Test
    void testMeasurementType_throwsOnUnexpected() {
        // Given
        var pr = new DatadisPermissionRequest(
                "pid",
                "cid",
                "dnid",
                Granularity.PT5M,
                "NIF",
                "mpid",
                LocalDate.now(ZoneOffset.UTC),
                LocalDate.now(ZoneOffset.UTC),
                null,
                null,
                null,
                PermissionProcessStatus.CREATED,
                null,
                false,
                ZonedDateTime.now(ZoneOffset.UTC)
        );

        // When
        // Then
        assertThrows(IllegalArgumentException.class, pr::measurementType);
    }
}