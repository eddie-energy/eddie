package energy.eddie.dataneeds.utils.cron;

import org.springframework.scheduling.support.CronExpression;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ValueDeserializer;

public class CronExpressionDeserializer extends ValueDeserializer<CronExpression> {
    @Override
    public CronExpression deserialize(
            tools.jackson.core.JsonParser p,
            tools.jackson.databind.DeserializationContext ctxt
    ) throws JacksonException {
        var cron = ctxt.readValue(p, String.class);
        return CronExpression.parse(cron);
    }
}
