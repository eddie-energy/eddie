package energy.eddie.regionconnector.dk.energinet.utils;

import energy.eddie.regionconnector.dk.energinet.enums.PeriodResolutionEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PeriodResolutionEnumConverterTest {

    @Test
    void invoke_parsesPeriodResolutionEnum() {
        // Given
        PeriodResolutionEnum dateTime = PeriodResolutionEnum.PT1H;
        PeriodResolutionEnumConverter converter = new PeriodResolutionEnumConverter();

        // When
        var res = converter.invoke("PT1H");

        // Then
        assertEquals(dateTime, res);
    }

    @Test
    void invoke_withNull_returnsNull() {
        // Given
        PeriodResolutionEnumConverter converter = new PeriodResolutionEnumConverter();

        // When
        var res = converter.invoke(null);

        // Then
        assertNull(res);
    }

    @Test
    void invoke_withMalformedString_returnsNull() {
        // Given
        PeriodResolutionEnumConverter converter = new PeriodResolutionEnumConverter();

        // When
        var res = converter.invoke("not a PeriodResolutionEnum string");

        // Then
        assertNull(res);
    }
}
