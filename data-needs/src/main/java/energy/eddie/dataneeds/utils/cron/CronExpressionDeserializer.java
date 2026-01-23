// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.dataneeds.utils.cron;

import org.springframework.scheduling.support.CronExpression;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

public class CronExpressionDeserializer extends ValueDeserializer<CronExpression> {
    @Override
    public CronExpression deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        var cron = ctxt.readValue(p, String.class);
        return CronExpression.parse(cron);
    }
}
