package energy.eddie.regionconnector.dk.energinet.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TimeSeriesAggregationEnumTest {
    @Test
    void fromPointQualityEnum_asExpected() {
        //given
        //when
        //then
        assertEquals(TimeSeriesAggregationEnum.QUARTER, TimeSeriesAggregationEnum.fromPointQualityEnum(PeriodResolutionEnum.PT15M));
        assertEquals(TimeSeriesAggregationEnum.HOUR, TimeSeriesAggregationEnum.fromPointQualityEnum(PeriodResolutionEnum.PT1H));
        assertEquals(TimeSeriesAggregationEnum.DAY, TimeSeriesAggregationEnum.fromPointQualityEnum(PeriodResolutionEnum.PT1D));
        assertEquals(TimeSeriesAggregationEnum.MONTH, TimeSeriesAggregationEnum.fromPointQualityEnum(PeriodResolutionEnum.P1M));
        assertEquals(TimeSeriesAggregationEnum.YEAR, TimeSeriesAggregationEnum.fromPointQualityEnum(PeriodResolutionEnum.P1Y));
    }

    @Test
    void fromString_asExpected() {
        //given
        //when
        //then
        assertEquals(TimeSeriesAggregationEnum.QUARTER, TimeSeriesAggregationEnum.fromString("Quarter"));
        assertEquals(TimeSeriesAggregationEnum.QUARTER, TimeSeriesAggregationEnum.fromString(" quarter "));
        assertEquals(TimeSeriesAggregationEnum.QUARTER, TimeSeriesAggregationEnum.fromString(" QuArTeR"));

        assertEquals(TimeSeriesAggregationEnum.HOUR, TimeSeriesAggregationEnum.fromString("Hour"));
        assertEquals(TimeSeriesAggregationEnum.HOUR, TimeSeriesAggregationEnum.fromString(" hour "));
        assertEquals(TimeSeriesAggregationEnum.HOUR, TimeSeriesAggregationEnum.fromString(" HoUr"));

        assertEquals(TimeSeriesAggregationEnum.DAY, TimeSeriesAggregationEnum.fromString("Day"));
        assertEquals(TimeSeriesAggregationEnum.DAY, TimeSeriesAggregationEnum.fromString(" day "));
        assertEquals(TimeSeriesAggregationEnum.DAY, TimeSeriesAggregationEnum.fromString(" DaY"));

        assertEquals(TimeSeriesAggregationEnum.MONTH, TimeSeriesAggregationEnum.fromString("Month"));
        assertEquals(TimeSeriesAggregationEnum.MONTH, TimeSeriesAggregationEnum.fromString(" month "));
        assertEquals(TimeSeriesAggregationEnum.MONTH, TimeSeriesAggregationEnum.fromString(" MoNtH"));

        assertEquals(TimeSeriesAggregationEnum.YEAR, TimeSeriesAggregationEnum.fromString("Year"));
        assertEquals(TimeSeriesAggregationEnum.YEAR, TimeSeriesAggregationEnum.fromString(" year "));
        assertEquals(TimeSeriesAggregationEnum.YEAR, TimeSeriesAggregationEnum.fromString(" YeAr"));

        assertEquals(TimeSeriesAggregationEnum.ACTUAL, TimeSeriesAggregationEnum.fromString("Actual"));
        assertEquals(TimeSeriesAggregationEnum.ACTUAL, TimeSeriesAggregationEnum.fromString(" actual "));
        assertEquals(TimeSeriesAggregationEnum.ACTUAL, TimeSeriesAggregationEnum.fromString(" AcTuAl"));
    }

    @Test
    void fromString_stringTooShort_throws() {
        //given
        //when
        //then
        assertThrows(IllegalArgumentException.class, () -> TimeSeriesAggregationEnum.fromString("Da"));
    }

    @Test
    void fromString_stringIsNull_throws() {
        //given
        //when
        //then
        assertThrows(NullPointerException.class, () -> TimeSeriesAggregationEnum.fromString(null));
    }
}
