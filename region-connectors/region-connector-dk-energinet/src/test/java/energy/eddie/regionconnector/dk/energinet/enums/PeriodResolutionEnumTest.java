package energy.eddie.regionconnector.dk.energinet.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PeriodResolutionEnumTest {
    @Test
    void fromString_asExpected() {
        //given
        //when
        //then
        assertEquals(PeriodResolutionEnum.PT15M, PeriodResolutionEnum.fromString("PT15M"));
        assertEquals(PeriodResolutionEnum.PT1H, PeriodResolutionEnum.fromString("PT1H"));
        assertEquals(PeriodResolutionEnum.PT1D, PeriodResolutionEnum.fromString("P1D"));
        assertEquals(PeriodResolutionEnum.PT1D, PeriodResolutionEnum.fromString("PT1D"));
        assertEquals(PeriodResolutionEnum.P1M, PeriodResolutionEnum.fromString("P1M"));
        assertEquals(PeriodResolutionEnum.P1Y, PeriodResolutionEnum.fromString("P1Y"));
    }

    @Test
    void fromString_invalidPeriodResolution_throws() {
        //given
        //when
        //then
        assertThrows(IllegalArgumentException.class, () -> PeriodResolutionEnum.fromString("PT1M"));
        assertThrows(IllegalArgumentException.class, () -> PeriodResolutionEnum.fromString("PT1Y"));
        assertThrows(IllegalArgumentException.class, () -> PeriodResolutionEnum.fromString(""));
    }

    @Test
    void fromString_periodResolutionIsNull_throws() {
        //given
        //when
        //then
        assertThrows(IllegalArgumentException.class, () -> PeriodResolutionEnum.fromString("PT1M"));
        assertThrows(IllegalArgumentException.class, () -> PeriodResolutionEnum.fromString("PT1Y"));
        assertThrows(IllegalArgumentException.class, () -> PeriodResolutionEnum.fromString(""));
    }

    @Test
    void fromTimeSeriesAggregation_asExpected() {
        //given
        //when
        //then
        assertEquals(PeriodResolutionEnum.PT15M, PeriodResolutionEnum.fromTimeSeriesAggregation(TimeSeriesAggregationEnum.QUARTER));
        assertEquals(PeriodResolutionEnum.PT1H, PeriodResolutionEnum.fromTimeSeriesAggregation(TimeSeriesAggregationEnum.HOUR));
        assertEquals(PeriodResolutionEnum.PT1H, PeriodResolutionEnum.fromTimeSeriesAggregation(TimeSeriesAggregationEnum.ACTUAL));
        assertEquals(PeriodResolutionEnum.PT1D, PeriodResolutionEnum.fromTimeSeriesAggregation(TimeSeriesAggregationEnum.DAY));
        assertEquals(PeriodResolutionEnum.P1M, PeriodResolutionEnum.fromTimeSeriesAggregation(TimeSeriesAggregationEnum.MONTH));
        assertEquals(PeriodResolutionEnum.P1Y, PeriodResolutionEnum.fromTimeSeriesAggregation(TimeSeriesAggregationEnum.YEAR));
    }
}
