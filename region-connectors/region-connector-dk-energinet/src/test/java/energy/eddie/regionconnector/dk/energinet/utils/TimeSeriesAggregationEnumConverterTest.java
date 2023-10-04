package energy.eddie.regionconnector.dk.energinet.utils;

import energy.eddie.regionconnector.dk.energinet.enums.TimeSeriesAggregationEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TimeSeriesAggregationEnumConverterTest {

    @Test
    void invoke_parsesTimeSeriesAggregationEnum() {
        // Given
        TimeSeriesAggregationEnum dateTime = TimeSeriesAggregationEnum.ACTUAL;
        TimeSeriesAggregationEnumConverter converter = new TimeSeriesAggregationEnumConverter();

        // When
        var res = converter.invoke("Actual");

        // Then
        assertEquals(dateTime, res);
    }

    @Test
    void invoke_withNull_returnsNull() {
        // Given
        TimeSeriesAggregationEnumConverter converter = new TimeSeriesAggregationEnumConverter();

        // When
        var res = converter.invoke(null);

        // Then
        assertNull(res);
    }

    @Test
    void invoke_withMalformedString_returnsNull() {
        // Given
        TimeSeriesAggregationEnumConverter converter = new TimeSeriesAggregationEnumConverter();

        // When
        var res = converter.invoke("not a TimeSeriesAggregationEnum string");

        // Then
        assertNull(res);
    }
}
