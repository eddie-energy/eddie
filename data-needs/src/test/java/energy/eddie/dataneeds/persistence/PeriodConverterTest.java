package energy.eddie.dataneeds.persistence;

import org.junit.jupiter.api.Test;

import java.time.Period;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class PeriodConverterTest {
    private final PeriodConverter converter = new PeriodConverter();

    @Test
    void givenValidPeriod_convertToDatabaseColumn_normalizesAndConvertsToString() {
        // Given
        Period period = Period.parse("P13M5D");

        // When
        String dbData = converter.convertToDatabaseColumn(period);

        // Then
        assertEquals("P1Y1M5D", dbData);
    }

    @Test
    void givenNegativeValidPeriod_convertToDatabaseColumn_normalizesAndConvertsToString() {
        // Given
        Period period = Period.parse("-P5Y1M6D");

        // When
        String dbData = converter.convertToDatabaseColumn(period);

        // Then; at least one - should be contained to indicate a negative period
        assertThat(dbData).contains("-");
    }

    @Test
    void givenNull_convertToDatabaseColumn_returnsNull() {
        // When
        String dbData = converter.convertToDatabaseColumn(null);

        // Then
        assertNull(dbData);
    }

    @Test
    void givenNull_convertToEntityAttribute_returnsNull() {
        // When
        Period period = converter.convertToEntityAttribute(null);

        // Then
        assertNull(period);
    }

    @Test
    void givenEmptyString_convertToEntityAttribute_returnsNull() {
        // When
        Period period = converter.convertToEntityAttribute("");

        // Then
        assertNull(period);
    }

    @Test
    @SuppressWarnings("JavaPeriodGetDays")
        // is intended
    void givenPeriodString_convertToEntityAttribute_returnsParsedPeriod() {
        // When
        Period period = converter.convertToEntityAttribute("-P2Y1M27D");

        // Then
        assertNotNull(period);
        assertEquals(-25, period.toTotalMonths());
        assertEquals(-27, period.getDays());
    }
}
