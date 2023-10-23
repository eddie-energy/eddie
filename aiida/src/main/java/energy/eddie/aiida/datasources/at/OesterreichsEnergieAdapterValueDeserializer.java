package energy.eddie.aiida.datasources.at;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import jakarta.annotation.Nullable;

import java.io.IOException;

public class OesterreichsEnergieAdapterValueDeserializer extends StdDeserializer<OesterreichAdapterJson.AdapterValue> {
    public OesterreichsEnergieAdapterValueDeserializer(@Nullable Class<?> vc) {
        super(vc);
    }

    @Override
    public OesterreichAdapterJson.AdapterValue deserialize(JsonParser jp, DeserializationContext context) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        // Try to convert the value to a Double, Integer or String
        JsonNode valueNode = node.get("value");
        Object value;

        if (valueNode.isInt()) {
            value = valueNode.intValue();
        } else {
            value = valueNode.asText();
        }

        JsonNode timeNode = node.get("time");
        long time = 0L;

        if (timeNode != null)
            time = timeNode.longValue();

        return new OesterreichAdapterJson.AdapterValue(value, time);
    }
}
