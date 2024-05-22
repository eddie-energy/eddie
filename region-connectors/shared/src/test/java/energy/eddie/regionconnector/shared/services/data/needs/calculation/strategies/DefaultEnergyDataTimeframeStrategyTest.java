package energy.eddie.regionconnector.shared.services.data.needs.calculation.strategies;

import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.TimeframedDataNeed;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultEnergyDataTimeframeStrategyTest {
    @Mock
    private AccountingPointDataNeed invalidDataNeed;
    @Mock
    private TimeframedDataNeed validDataNeed;
    @Mock
    private RelativeDuration duration;
    @Mock
    private RegionConnectorMetadata metadata;

    @Test
    void testEnergyData_returnsNull_onInvalidDataNeed() throws UnsupportedDataNeedException {
        // Given
        var strategy = new DefaultEnergyDataTimeframeStrategy(metadata);

        // When
        var res = strategy.energyDataTimeframe(invalidDataNeed);

        // Then
        assertNull(res);
    }

    @Test
    void testEnergyData_returnsCorrectTimeframe() throws UnsupportedDataNeedException {
        // Given
        var strategy = new DefaultEnergyDataTimeframeStrategy(metadata);
        when(validDataNeed.duration())
                .thenReturn(duration);
        when(duration.start())
                .thenReturn(Optional.empty());
        when(duration.end())
                .thenReturn(Optional.empty());
        when(metadata.earliestStart())
                .thenReturn(Period.ofDays(1));
        when(metadata.latestEnd())
                .thenReturn(Period.ofDays(10));
        when(metadata.timeZone())
                .thenReturn(ZoneOffset.UTC);
        var now = LocalDate.now(ZoneOffset.UTC);

        // When
        var res = strategy.energyDataTimeframe(validDataNeed);

        // Then
        assertAll(
                () -> assertNotNull(res),
                () -> assertEquals(now.plusDays(1), res.start()),
                () -> assertEquals(now.plusDays(10), res.end())
        );
    }
}