// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.dataneeds.utils.cron;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.scheduling.support.CronExpression;

import static org.junit.jupiter.api.Assertions.*;

class CronExpressionConverterTest {
    @Test
    void testConvertToEntityAttribute_withValidCronExpression_returnsCronExpression() {
        // Given
        String validCronExpression = "0 0 12 * * ?";
        CronExpressionConverter converter = new CronExpressionConverter();

        // When
        CronExpression result = converter.convertToEntityAttribute(validCronExpression);

        // Then
        assertNotNull(result);
        assertEquals(validCronExpression, result.toString());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {" ", ""})
    void testConvertToEntityAttribute_withNullInput_returnsNull(String expression) {
        // Given
        CronExpressionConverter converter = new CronExpressionConverter();

        // When
        CronExpression result = converter.convertToEntityAttribute(expression);

        // Then
        assertNull(result);
    }

    @Test
    void testConvertToEntityAttribute_withInvalidCronExpression_throwsException() {
        // Given
        String invalidCronExpression = "INVALID_CRON";
        CronExpressionConverter converter = new CronExpressionConverter();

        // When / Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                                                          () -> converter.convertToEntityAttribute(invalidCronExpression));
        assertTrue(exception.getMessage().contains("Could not convert database value to CronExpression"));
    }
}