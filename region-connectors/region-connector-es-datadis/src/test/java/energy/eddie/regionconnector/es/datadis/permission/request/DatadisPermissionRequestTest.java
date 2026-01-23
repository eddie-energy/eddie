// SPDX-FileCopyrightText: 2023-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.permission.request;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.es.datadis.DatadisPermissionRequestBuilder;
import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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
        var pr = new DatadisPermissionRequestBuilder()
                .setGranularity(granularity)
                .build();

        // When
        var res = pr.measurementType();

        // Then
        assertEquals(measurementType, res);
    }

    @Test
    void testMeasurementType_throwsOnUnexpected() {
        // Given
        var pr = new DatadisPermissionRequestBuilder()
                .setGranularity(Granularity.PT5M)
                .build();

        // When
        // Then
        assertThrows(IllegalStateException.class, pr::measurementType);
    }
}
