package energy.eddie.dataneeds.utils.cron;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.scheduling.support.CronExpression;

import java.io.IOException;

public class CronExpressionDeserializer extends JsonDeserializer<CronExpression> {
    @Override
    public CronExpression deserialize(
            JsonParser jsonParser,
            DeserializationContext deserializationContext
    ) throws IOException {
        ObjectCodec oc = jsonParser.getCodec();
        JsonNode node = oc.readTree(jsonParser);
        return CronExpression.parse(node.asText(CronExpressionDefaults.SECONDLY.expression()));
    }
}
