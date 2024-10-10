package energy.eddie.regionconnector.be.fluvius.data.needs;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.dataneeds.EnergyType;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.regionconnector.be.fluvius.FluviusRegionConnectorMetadata;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FluviusEnergyTimeframeStrategyTest {

    private final FluviusEnergyTimeframeStrategy strategy = new FluviusEnergyTimeframeStrategy(
            FluviusRegionConnectorMetadata.getInstance()
    );

    @Test
    void testEnergyDataTimeframe_forAccountingPointData_returnsEndPlusOne() throws UnsupportedDataNeedException {
        // Given
        var dataNeed = new AccountingPointDataNeed();

        // When
        var res = strategy.energyDataTimeframe(dataNeed);

        // Then
        assertNotNull(res);
        assertNotNull(res.start());
        assertEquals(res.start().plusDays(1), res.end());
    }

    @Test
    void testEnergyDataTimeframe_forValidatedHistoricalData_returnsEndPlusOne() throws UnsupportedDataNeedException {
        // Given
        var dataNeed = new ValidatedHistoricalDataDataNeed(
                new RelativeDuration(Period.ofDays(-10), Period.ofDays(-1), null),
                EnergyType.ELECTRICITY,
                Granularity.PT15M,
                Granularity.P1D
        );

        // When
        var res = strategy.energyDataTimeframe(dataNeed);

        // Then
        assertNotNull(res);
        assertEquals(LocalDate.now(ZoneOffset.UTC), res.end());
    }
}