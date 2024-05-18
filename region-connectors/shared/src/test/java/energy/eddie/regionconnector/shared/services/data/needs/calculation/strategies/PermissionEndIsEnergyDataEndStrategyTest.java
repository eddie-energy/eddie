package energy.eddie.regionconnector.shared.services.data.needs.calculation.strategies;

import energy.eddie.api.agnostic.data.needs.Timeframe;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PermissionEndIsEnergyDataEndStrategyTest {
    @Test
    void testPermissionTimeframe_withEmptyTimeframe() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var strategy = new PermissionEndIsEnergyDataEndStrategy(ZoneOffset.UTC);

        // When
        var res = strategy.permissionTimeframe(null);

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
        var strategy = new PermissionEndIsEnergyDataEndStrategy(ZoneOffset.UTC);

        // When
        var res = strategy.permissionTimeframe(timeframe);

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
        var strategy = new PermissionEndIsEnergyDataEndStrategy(ZoneOffset.UTC);

        // When
        var res = strategy.permissionTimeframe(timeframe);

        // Then
        assertAll(
                () -> assertEquals(now, res.start()),
                () -> assertEquals(now.plusDays(100), res.end())
        );
    }
}