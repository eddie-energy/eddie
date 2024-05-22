package energy.eddie.regionconnector.aiida.data.needs;

import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.aiida.GenericAiidaDataNeed;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

import static energy.eddie.regionconnector.aiida.AiidaRegionConnectorMetadata.REGION_CONNECTOR_ZONE_ID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiidaEnergyDataTimeframeStrategyTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2023-10-15T15:00:00Z"),
                                                         REGION_CONNECTOR_ZONE_ID);
    @Mock
    private AccountingPointDataNeed accountingPointDataNeed;
    @Mock
    private GenericAiidaDataNeed genericAiidaDataNeed;
    @Mock
    private RelativeDuration duration;

    @Test
    void testEnergyDataTimeFrame_throwsOnInvalidDataNeed() {
        // Given
        var strategy = new AiidaEnergyDataTimeframeStrategy(FIXED_CLOCK);

        // When
        // Then
        assertThrows(UnsupportedDataNeedException.class, () -> strategy.energyDataTimeframe(accountingPointDataNeed));
    }

    @Test
    void testEnergyDataTimeframe_calculatesCorrectStartAndEndDate() throws UnsupportedDataNeedException {
        // Given
        var strategy = new AiidaEnergyDataTimeframeStrategy(FIXED_CLOCK);
        when(genericAiidaDataNeed.duration())
                .thenReturn(duration);
        when(duration.start())
                .thenReturn(Optional.of(Period.ofDays(1)));
        when(duration.end())
                .thenReturn(Optional.of(Period.ofDays(10)));
        // When
        var res = strategy.energyDataTimeframe(genericAiidaDataNeed);

        // Then
        assertAll(
                () -> assertNotNull(res),
                () -> assertEquals(LocalDate.of(2023, 10, 16), res.start()),
                () -> assertEquals(LocalDate.of(2023, 10, 25), res.end())
        );
    }

    @Test
    void testEnergyDataTimeframe_calculatesCorrectStartAndEndDate_withNullStartAndEnd() throws UnsupportedDataNeedException {
        // Given
        var strategy = new AiidaEnergyDataTimeframeStrategy(FIXED_CLOCK);
        when(genericAiidaDataNeed.duration())
                .thenReturn(duration);
        when(duration.start())
                .thenReturn(Optional.empty());
        when(duration.end())
                .thenReturn(Optional.empty());
        // When
        var res = strategy.energyDataTimeframe(genericAiidaDataNeed);

        // Then
        assertAll(
                () -> assertNotNull(res),
                () -> assertEquals(LocalDate.of(2000, 1, 1), res.start()),
                () -> assertEquals(LocalDate.of(9999, 12, 31), res.end())
        );
    }
}