package energy.eddie.aiida.datasources.at;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import jakarta.annotation.Nullable;

import java.io.IOException;

public class OesterreichsEnergieAdapterValueDeserializer extends StdDeserializer<OesterreichsEnergieAdapterJson.AdapterValue> {
    public OesterreichsEnergieAdapterValueDeserializer(@Nullable Class<?> vc) {
        super(vc);
    }

    @Override
    public OesterreichsEnergieAdapterJson.AdapterValue deserialize(JsonParser jp, DeserializationContext context) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        // use value either as Integer or String
        JsonNode valueNode = node.get("value");
        Object value = valueNode.isInt()
                ? valueNode.intValue()
                : valueNode.asText();

        JsonNode timeNode = node.get("time");
        long time = timeNode != null
                ? timeNode.longValue()
                : 0;

        return new OesterreichsEnergieAdapterJson.AdapterValue(value, time);
    }
}
