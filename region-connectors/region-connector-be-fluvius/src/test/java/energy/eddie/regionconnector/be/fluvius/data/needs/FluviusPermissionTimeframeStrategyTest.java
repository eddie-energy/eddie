// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.data.needs;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.agnostic.data.needs.Timeframe;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FluviusPermissionTimeframeStrategyTest {

    private final FluviusPermissionTimeframeStrategy strategy = new FluviusPermissionTimeframeStrategy();
    private final ValidatedHistoricalDataDataNeed dataNeed = new ValidatedHistoricalDataDataNeed(
            new RelativeDuration(null, null, null),
            EnergyType.ELECTRICITY,
            Granularity.PT15M,
            Granularity.P1D
    );

    @Test
    void testEnergyDataTimeframe_forAccountingPointData_returnsEndPlusOne() {
        // Given

        // When
        var res = strategy.permissionTimeframe(null, dataNeed, ZonedDateTime.now(ZoneOffset.UTC));

        // Then
        assertNotNull(res);
        assertNotNull(res.start());
        assertEquals(res.start().plusDays(1), res.end());
    }

    @Test
    void testEnergyDataTimeframe_forValidatedHistoricalData_returnsEnd() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var energyDataTimeframe = new Timeframe(now.minusDays(10), now.minusDays(1));

        // When
        var res = strategy.permissionTimeframe(energyDataTimeframe, dataNeed, now.atStartOfDay(ZoneOffset.UTC));

        // Then
        assertNotNull(res);
        assertEquals(now, res.end());
    }

    @Test
    void testEnergyDataTimeframe_forValidatedHistoricalData_returnsStartEqualReferenceDateTimeForFutureData() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var energyDataTimeframe = new Timeframe(now.plusDays(1), now.plusDays(10));

        // When
        var res = strategy.permissionTimeframe(energyDataTimeframe, dataNeed, now.atStartOfDay(ZoneOffset.UTC));

        // Then
        assertEquals(now, res.start());
    }
}