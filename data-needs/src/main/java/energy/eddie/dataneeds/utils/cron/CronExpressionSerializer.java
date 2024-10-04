package energy.eddie.dataneeds.utils.cron;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import org.springframework.scheduling.support.CronExpression;

import java.io.IOException;

public class CronExpressionSerializer extends JsonSerializer<CronExpression> {
    @Override
    public void serialize(
            CronExpression cronExpression,
            JsonGenerator jsonGenerator,
            SerializerProvider serializers
    ) throws IOException {
        jsonGenerator.writeString(cronExpression.toString());
    }
}
