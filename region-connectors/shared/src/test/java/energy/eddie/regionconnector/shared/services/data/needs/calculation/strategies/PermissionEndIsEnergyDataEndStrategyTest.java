// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.services.data.needs.calculation.strategies;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.agnostic.data.needs.Timeframe;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PermissionEndIsEnergyDataEndStrategyTest {
    private final ValidatedHistoricalDataDataNeed dataNeed = new ValidatedHistoricalDataDataNeed(
            new RelativeDuration(null, null, null),
            EnergyType.ELECTRICITY,
            Granularity.PT15M,
            Granularity.P1D
    );
    @Test
    void testPermissionTimeframe_withEmptyTimeframe() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var strategy = new PermissionEndIsEnergyDataEndStrategy();

        // When
        var res = strategy.permissionTimeframe(null, dataNeed, ZonedDateTime.now(ZoneOffset.UTC));

        // Then
        assertAll(
                () -> assertEquals(now, res.start()),
                () -> assertEquals(now, res.end())
        );
    }

    @Test
    void testPermissionTimeframe_withPastTimeframe() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var timeframe = new Timeframe(now.minusDays(10), now.minusDays(1));
        var strategy = new PermissionEndIsEnergyDataEndStrategy();

        // When
        var res = strategy.permissionTimeframe(timeframe, dataNeed, ZonedDateTime.now(ZoneOffset.UTC));

        // Then
        assertAll(
                () -> assertEquals(now, res.start()),
                () -> assertEquals(now, res.end())
        );
    }

    @Test
    void testPermissionTimeframe_withFutureTimeframe() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var timeframe = new Timeframe(now.plusDays(10), now.plusDays(100));
        var strategy = new PermissionEndIsEnergyDataEndStrategy();

        // When
        var res = strategy.permissionTimeframe(timeframe, dataNeed, ZonedDateTime.now(ZoneOffset.UTC));

        // Then
        assertAll(
                () -> assertEquals(now, res.start()),
                () -> assertEquals(now.plusDays(100), res.end())
        );
    }
}