package energy.eddie.regionconnector.es.datadis.data.needs.calculation.strategies;

import energy.eddie.api.agnostic.data.needs.Timeframe;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DatadisStrategyTest {

    @Test
    void testCalculation_withEmptyTimeframe() {
        // Given
        var now = LocalDate.now(ZONE_ID_SPAIN);
        var strategy = new DatadisStrategy();

        // When
        var res = strategy.permissionTimeframe(null);

        // Then
        assertAll(
                () -> assertEquals(now, res.start()),
                () -> assertEquals(now.plusDays(1), res.end())
        );
    }

    @Test
    void testCalculation_withPastEndDate() {
        // Given
        var now = LocalDate.now(ZONE_ID_SPAIN);
        var timeframe = new Timeframe(now.minusDays(10), now.minusDays(3));
        var strategy = new DatadisStrategy();

        // When
        var res = strategy.permissionTimeframe(timeframe);

        // Then
        assertAll(
                () -> assertEquals(now, res.start()),
                () -> assertEquals(now.plusDays(1), res.end())
        );
    }

    @Test
    void testCalculation_withFutureEndDate() {
        // Given
        var now = LocalDate.now(ZONE_ID_SPAIN);
        var timeframe = new Timeframe(now.minusDays(10), now.plusDays(3));
        var strategy = new DatadisStrategy();

        // When
        var res = strategy.permissionTimeframe(timeframe);

        // Then
        assertAll(
                () -> assertEquals(now, res.start()),
                () -> assertEquals(now.plusDays(4), res.end())
        );
    }
}