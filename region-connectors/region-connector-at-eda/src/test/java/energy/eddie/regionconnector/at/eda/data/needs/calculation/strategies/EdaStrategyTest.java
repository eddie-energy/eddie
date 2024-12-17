package energy.eddie.regionconnector.at.eda.data.needs.calculation.strategies;

import energy.eddie.dataneeds.duration.AbsoluteDuration;
import energy.eddie.dataneeds.duration.DataNeedDuration;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.TimeframedDataNeed;
import energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EdaStrategyTest {
    @Mock
    private TimeframedDataNeed timeframedDataNeed;
    @Mock
    private AccountingPointDataNeed accountingPointDataNeed;

    private static Stream<Arguments> unsupportedDataNeedDurations() {
        LocalDate now = LocalDate.now(AT_ZONE_ID);
        return Stream.of(
                Arguments.of(relativeDuration(EdaRegionConnectorMetadata.PERIOD_EARLIEST_START.minusMonths(1),
                                              EdaRegionConnectorMetadata.PERIOD_EARLIEST_START.plusMonths(1)),
                             "Relative exceeds max past"),
                Arguments.of(new AbsoluteDuration(now.minusYears(5), now.minusMonths(1)),
                             "Absolut exceeds max past"),
                Arguments.of(relativeDuration(EdaRegionConnectorMetadata.PERIOD_LATEST_END,
                                              EdaRegionConnectorMetadata.PERIOD_LATEST_END.plusMonths(1)),
                             "Relative exceeds max future"),
                Arguments.of(new AbsoluteDuration(now.plusMonths(1), now.plusYears(5)),
                             "Absolut exceeds max future"),
                Arguments.of(relativeDuration(EdaRegionConnectorMetadata.PERIOD_EARLIEST_START.plusMonths(1),
                                              EdaRegionConnectorMetadata.PERIOD_LATEST_END.minusMonths(1)),
                             "Relative from past to future"),
                Arguments.of(new AbsoluteDuration(now.minusMonths(1), now.plusMonths(1)),
                             "Absolut from past to future")
        );
    }

    private static RelativeDuration relativeDuration(Period start, Period end) {
        return new RelativeDuration(start, end, null);
    }

    private static Stream<Arguments> supportedDataNeedDuration() {
        LocalDate now = LocalDate.now(AT_ZONE_ID);
        return Stream.of(
                Arguments.of(relativeDuration(EdaRegionConnectorMetadata.PERIOD_EARLIEST_START, Period.ofDays(-1)),
                             "Maximum supported past",
                             now.plusMonths(EdaRegionConnectorMetadata.PERIOD_EARLIEST_START.getMonths()),
                             now.minusDays(1)),
                Arguments.of(new AbsoluteDuration(now.minusYears(2), now.minusMonths(1)),
                             "2 years to last month in the past", now.minusYears(2), now.minusMonths(1)),
                Arguments.of(relativeDuration(Period.ZERO, EdaRegionConnectorMetadata.PERIOD_LATEST_END),
                             "Maximum supported future",
                             now,
                             now.plusMonths(EdaRegionConnectorMetadata.PERIOD_LATEST_END.getMonths())),
                Arguments.of(new AbsoluteDuration(now, now.plusYears(2)),
                             "Today to 2 years in the future", now, now.plusYears(2))
        );
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("unsupportedDataNeedDurations")
    void energyDataTimeframe_throwsIfDateExceedMaxPast(DataNeedDuration duration, String message) {
        // Given
        when(timeframedDataNeed.duration()).thenReturn(duration);
        EdaStrategy edaStrategy = new EdaStrategy();

        // When & Then
        assertThrows(UnsupportedDataNeedException.class, () -> edaStrategy.energyDataTimeframe(timeframedDataNeed,
                                                                                               ZonedDateTime.now(
                                                                                                       ZoneOffset.UTC)));
    }

    @SuppressWarnings("DataFlowIssue")
    @ParameterizedTest(name = "{1}")
    @MethodSource("supportedDataNeedDuration")
    void energyDataTimeframe_doesNotThrowIfDateWithinMaxPast(
            DataNeedDuration duration,
            String message,
            LocalDate expectedStart,
            LocalDate expectedEnd
    ) throws UnsupportedDataNeedException {
        // Given
        when(timeframedDataNeed.duration()).thenReturn(duration);
        EdaStrategy edaStrategy = new EdaStrategy();

        // When
        var timeFrame = edaStrategy.energyDataTimeframe(timeframedDataNeed, ZonedDateTime.now(ZoneOffset.UTC));

        // Then
        assertAll(
                () -> assertNotNull(timeFrame),
                () -> assertEquals(expectedStart, timeFrame.start()),
                () -> assertEquals(expectedEnd, timeFrame.end())
        );
    }

    @Test
    void energyDataTimeframe_returnsNullIfAccountingPointDataNeed() throws UnsupportedDataNeedException {
        // Given
        EdaStrategy edaStrategy = new EdaStrategy();

        // When
        var timeFrame = edaStrategy.energyDataTimeframe(accountingPointDataNeed, ZonedDateTime.now(ZoneOffset.UTC));

        // Then
        assertNull(timeFrame);
    }
}
