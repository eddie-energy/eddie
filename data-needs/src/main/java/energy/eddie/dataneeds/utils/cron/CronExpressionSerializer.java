package energy.eddie.dataneeds.utils.cron;

import org.springframework.scheduling.support.CronExpression;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

public class CronExpressionSerializer extends ValueSerializer<CronExpression> {
    @Override
    public void serialize(CronExpression value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
        gen.writeString(value.toString());
    }
}
