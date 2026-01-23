// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.client;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.be.fluvius.permission.request.Flow;
import energy.eddie.regionconnector.be.fluvius.permission.request.FluviusPermissionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DataServiceTypeTest {

    public static Stream<Arguments> testFrom_returnsCorrectGranularity() {
        return Stream.of(
                Arguments.of(Granularity.PT15M, DataServiceType.QUARTER_HOURLY),
                Arguments.of(Granularity.PT30M, DataServiceType.HALF_HOURLY),
                Arguments.of(Granularity.P1D, DataServiceType.DAILY)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testFrom_returnsCorrectGranularity(Granularity granularity, DataServiceType dataServiceType) {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var pr = new FluviusPermissionRequest(
                "pid",
                "cid",
                "dnid",
                PermissionProcessStatus.VALIDATED,
                granularity,
                now,
                now,
                now.atStartOfDay(ZoneOffset.UTC),
                Flow.B2B
        );

        // When
        var res = DataServiceType.from(pr);

        // Then
        assertEquals(dataServiceType, res);
    }

    @Test
    void testFrom_throwsOnInvalidGranularity() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var pr = new FluviusPermissionRequest(
                "pid",
                "cid",
                "dnid",
                PermissionProcessStatus.VALIDATED,
                Granularity.P1Y,
                now,
                now,
                now.atStartOfDay(ZoneOffset.UTC),
                Flow.B2B
        );

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> DataServiceType.from(pr));
    }
}