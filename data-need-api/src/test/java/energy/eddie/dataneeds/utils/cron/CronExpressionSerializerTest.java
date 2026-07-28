// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.dataneeds.utils.cron;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.support.CronExpression;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CronExpressionSerializerTest {
    @Mock
    private JsonGenerator jsonGenerator;
    @Mock
    private SerializationContext ctx;

    @Test
    void testSerialize_withValidCronExpression_serializesCorrectly() {
        // Given
        CronExpression cron = CronExpression.parse("0 0 12 * * ?");
        CronExpressionSerializer serializer = new CronExpressionSerializer();

        // When
        serializer.serialize(cron, jsonGenerator, ctx);

        // Then
        verify(jsonGenerator, times(1)).writeString("0 0 12 * * ?");
    }
}