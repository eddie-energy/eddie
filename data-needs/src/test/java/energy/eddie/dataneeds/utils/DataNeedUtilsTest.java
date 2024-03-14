package energy.eddie.dataneeds.utils;

import energy.eddie.dataneeds.duration.AbsoluteDuration;
import energy.eddie.dataneeds.duration.CalendarUnit;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.TimeframedDataNeed;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataNeedUtilsTest {
    @Mock
    private AccountingPointDataNeed accountDataNeed;
    @Mock
    private TimeframedDataNeed dataNeed;
    @Mock
    private AbsoluteDuration absoluteDuration;
    @Mock
    private RelativeDuration relativeDuration;
    private final LocalDate start = LocalDate.of(2024, 4, 1);
    private final LocalDate end = LocalDate.of(2024, 4, 25);
    private final LocalDate referenceDate = LocalDate.of(2024, 4, 10);
    private final Period earliestStart = Period.parse("-P2Y");
    private final Period latestEnd = Period.parse("P2Y");
    private final Period desiredRelativeStart = Period.parse("-P5D");
    private final Period desiredRelativeEnd = Period.parse("P18D");

    @Test
    void givenAccountDataNeed_throws() {
        // Given
        when(accountDataNeed.id()).thenReturn("someId");

        // When
        assertThrows(IllegalArgumentException.class,
                     () -> DataNeedUtils.calculateRelativeStartAndEnd(accountDataNeed, null, null, null));
    }

    @Test
    void givenAbsoluteDuration_returnsAbsoluteDates() {
        // Given
        when(dataNeed.duration()).thenReturn(absoluteDuration);
        when(absoluteDuration.start()).thenReturn(start);
        when(absoluteDuration.end()).thenReturn(end);

        // When
        DataNeedWrapper wrapper = DataNeedUtils.calculateRelativeStartAndEnd(dataNeed, null, null, null);

        // Then
        assertEquals(start, wrapper.calculatedStart());
        assertEquals(end, wrapper.calculatedEnd());
    }

    @Test
    void givenOpenStart_returnsOpenStartDate() {
        // Given
        when(dataNeed.duration()).thenReturn(relativeDuration);
        when(relativeDuration.start()).thenReturn(Optional.empty());
        when(relativeDuration.end()).thenReturn(Optional.of(desiredRelativeEnd));

        // When
        DataNeedWrapper wrapper = DataNeedUtils.calculateRelativeStartAndEnd(dataNeed,
                                                                             referenceDate,
                                                                             earliestStart,
                                                                             latestEnd);

        // Then
        assertEquals(LocalDate.of(2022, 4, 10), wrapper.calculatedStart());
        assertEquals(LocalDate.of(2024, 4, 28), wrapper.calculatedEnd());
    }

    @Test
    void givenOpenEnd_returnsOpenEndDate() {
        // Given
        when(dataNeed.duration()).thenReturn(relativeDuration);
        when(relativeDuration.start()).thenReturn(Optional.of(desiredRelativeStart));
        when(relativeDuration.end()).thenReturn(Optional.empty());

        // When
        DataNeedWrapper wrapper = DataNeedUtils.calculateRelativeStartAndEnd(dataNeed,
                                                                             referenceDate,
                                                                             earliestStart,
                                                                             latestEnd);

        // Then
        assertEquals(LocalDate.of(2024, 4, 5), wrapper.calculatedStart());
        assertEquals(LocalDate.of(2026, 4, 10), wrapper.calculatedEnd());
    }

    @Test
    void givenOpenStartAndEnd_returnsOpenStartAndEndDate() {
        // Given
        when(dataNeed.duration()).thenReturn(relativeDuration);
        when(relativeDuration.start()).thenReturn(Optional.empty());
        when(relativeDuration.end()).thenReturn(Optional.empty());

        // When
        DataNeedWrapper wrapper = DataNeedUtils.calculateRelativeStartAndEnd(dataNeed,
                                                                             referenceDate,
                                                                             earliestStart,
                                                                             latestEnd);

        // Then
        assertEquals(LocalDate.of(2022, 4, 10), wrapper.calculatedStart());
        assertEquals(LocalDate.of(2026, 4, 10), wrapper.calculatedEnd());
    }

    @Test
    void givenOpenEndAndStickyStartWeek_returnsAsExpected() {
        // Given
        when(dataNeed.duration()).thenReturn(relativeDuration);
        when(relativeDuration.start()).thenReturn(Optional.of(desiredRelativeStart));
        when(relativeDuration.end()).thenReturn(Optional.empty());
        when(relativeDuration.stickyStartCalendarUnit()).thenReturn(Optional.of(CalendarUnit.WEEK));

        // When
        DataNeedWrapper wrapper = DataNeedUtils.calculateRelativeStartAndEnd(dataNeed,
                                                                             referenceDate,
                                                                             earliestStart,
                                                                             latestEnd);

        // Then
        assertEquals(LocalDate.of(2024, 4, 1), wrapper.calculatedStart());
        assertEquals(LocalDate.of(2026, 4, 10), wrapper.calculatedEnd());
    }

    @Test
    void givenEndAndStickyStartMonth_returnsAsExpected() {
        // Given
        var otherDesiredRelativeStart = Period.parse("-P15D");
        when(dataNeed.duration()).thenReturn(relativeDuration);
        when(relativeDuration.start()).thenReturn(Optional.of(otherDesiredRelativeStart));
        when(relativeDuration.end()).thenReturn(Optional.of(desiredRelativeEnd));
        when(relativeDuration.stickyStartCalendarUnit()).thenReturn(Optional.of(CalendarUnit.MONTH));

        // When
        DataNeedWrapper wrapper = DataNeedUtils.calculateRelativeStartAndEnd(dataNeed,
                                                                             referenceDate,
                                                                             earliestStart,
                                                                             latestEnd);

        // Then
        assertEquals(LocalDate.of(2024, 3, 1), wrapper.calculatedStart());
        assertEquals(LocalDate.of(2024, 4, 28), wrapper.calculatedEnd());
    }

    @Test
    void givenEndAndStickyStartYear_returnsAsExpected() {
        // Given
        var otherDesiredRelativeStart = Period.parse("-P15D");
        when(dataNeed.duration()).thenReturn(relativeDuration);
        when(relativeDuration.start()).thenReturn(Optional.of(otherDesiredRelativeStart));
        when(relativeDuration.end()).thenReturn(Optional.of(desiredRelativeEnd));
        when(relativeDuration.stickyStartCalendarUnit()).thenReturn(Optional.of(CalendarUnit.YEAR));

        // When
        DataNeedWrapper wrapper = DataNeedUtils.calculateRelativeStartAndEnd(dataNeed,
                                                                             referenceDate,
                                                                             earliestStart,
                                                                             latestEnd);

        // Then
        assertEquals(LocalDate.of(2024, 1, 1), wrapper.calculatedStart());
        assertEquals(LocalDate.of(2024, 4, 28), wrapper.calculatedEnd());
    }
}
