package energy.eddie.regionconnector.dk.energinet.enums;

import energy.eddie.api.agnostic.Granularity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TimeSeriesAggregationEnumTest {
    @Test
    void fromGranularity_asExpected() {
        assertEquals(TimeSeriesAggregationEnum.QUARTER, TimeSeriesAggregationEnum.fromGranularity(Granularity.PT15M));
        assertEquals(TimeSeriesAggregationEnum.HOUR, TimeSeriesAggregationEnum.fromGranularity(Granularity.PT1H));
        assertEquals(TimeSeriesAggregationEnum.DAY, TimeSeriesAggregationEnum.fromGranularity(Granularity.P1D));
        assertEquals(TimeSeriesAggregationEnum.MONTH, TimeSeriesAggregationEnum.fromGranularity(Granularity.P1M));
        assertEquals(TimeSeriesAggregationEnum.YEAR, TimeSeriesAggregationEnum.fromGranularity(Granularity.P1Y));
    }

    @Test
    void fromGranularity_ifUnsopportedGranularity_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> TimeSeriesAggregationEnum.fromGranularity(Granularity.PT5M));
    }
}