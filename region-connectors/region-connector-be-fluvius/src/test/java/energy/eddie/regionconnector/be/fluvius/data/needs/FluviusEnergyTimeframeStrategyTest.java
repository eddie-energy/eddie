package energy.eddie.regionconnector.be.fluvius.data.needs;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.regionconnector.be.fluvius.FluviusRegionConnectorMetadata;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FluviusEnergyTimeframeStrategyTest {

    private final FluviusEnergyTimeframeStrategy strategy = new FluviusEnergyTimeframeStrategy(
            new FluviusRegionConnectorMetadata()
    );

    @Test
    void testEnergyDataTimeframe_forAccountingPointData_returnsEndPlusOne() throws UnsupportedDataNeedException {
        // Given
        var dataNeed = new AccountingPointDataNeed();
        var reference = ZonedDateTime.now(ZoneOffset.UTC);

        // When
        var res = strategy.energyDataTimeframe(dataNeed, reference);

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
        var reference = ZonedDateTime.now(ZoneOffset.UTC);

        // When
        var res = strategy.energyDataTimeframe(dataNeed, reference);

        // Then
        assertNotNull(res);
        assertEquals(LocalDate.now(ZoneOffset.UTC), res.end());
    }
}
