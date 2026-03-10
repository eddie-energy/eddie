// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.data.needs.calculation.strategies;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.agnostic.data.needs.Timeframe;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.needs.CESUJoinRequestDataNeed;
import energy.eddie.dataneeds.needs.EnergyDirection;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;


class EdaPermissionTimeframeStrategyTest {
    private final EdaPermissionTimeframeStrategy strategy = new EdaPermissionTimeframeStrategy();

    @Test
    void givenValidatedHistoricalDataNeed_whenPermissionTimeframe_thenReturnPermissionIsEnergyTimeFrame() {
        // Given
        ValidatedHistoricalDataDataNeed dataNeed = new ValidatedHistoricalDataDataNeed(
                new RelativeDuration(null, null, null),
                EnergyType.ELECTRICITY,
                Granularity.PT15M,
                Granularity.P1D
        );
        var energyTimeframe = new Timeframe(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2));

        // When
        var res = strategy.permissionTimeframe(
                energyTimeframe,
                dataNeed,
                ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
        );

        // Then
        assertEquals(energyTimeframe, res);
    }

    @Test
    void givenEnergyCommunityDataNeed_whenPermissionTimeframe_thenReturnStartNextDayAndEndNull() {
        // Given
        var dataNeed = new CESUJoinRequestDataNeed(100,
                                                   Granularity.PT15M,
                                                   Granularity.P1D,
                                                   EnergyDirection.CONSUMPTION);
        var expected = new Timeframe(LocalDate.of(2026, 1, 2), null);

        // When
        var res = strategy.permissionTimeframe(
                null,
                dataNeed,
                ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
        );

        // Then
        assertEquals(expected, res);
    }
}