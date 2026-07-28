// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.dataneeds.utils.cron;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.support.CronExpression;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CronExpressionDeserializerTest {
    @Mock
    private JsonParser parser;
    @Mock
    private DeserializationContext ctx;

    @Test
    void givenValidCronString_whenDeserialize_thenReturnsValidCronExpression() {
        // Given
        String cronString = "0 0 12 * * ?";

        when(ctx.readValue(parser, String.class)).thenReturn(cronString);

        CronExpressionDeserializer deserializer = new CronExpressionDeserializer();

        // When
        CronExpression result = deserializer.deserialize(parser, ctx);

        // Then
        assertEquals(CronExpression.parse(cronString), result);
    }

    @Test
    void givenInvalidCronString_whenDeserialize_thenThrowsIllegalArgumentException() {
        // Given
        String invalidCronString = "invalid cron";

        when(ctx.readValue(parser, String.class)).thenReturn(invalidCronString);

        CronExpressionDeserializer deserializer = new CronExpressionDeserializer();

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> deserializer.deserialize(parser, ctx));
    }
}