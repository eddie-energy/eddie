package energy.eddie.aiida.datasources.fr;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import jakarta.annotation.Nullable;

import java.io.IOException;

public class MicroTeleinfoV3ValueDeserializer extends StdDeserializer<MicroTeleinfoV3Json.TeleinfoDataField> {
    public MicroTeleinfoV3ValueDeserializer(@Nullable Class<?> vc) {
        super(vc);
    }

    @Override
    public MicroTeleinfoV3Json.TeleinfoDataField deserialize(
            JsonParser jp,
            DeserializationContext context
    ) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        // use value either as Integer or String
        String raw = node.get("raw").asText();
        JsonNode valueNode = node.get("value");
        Object value = valueNode.isInt()
                ? valueNode.intValue()
                : valueNode.asText();

        return new MicroTeleinfoV3Json.TeleinfoDataField(raw, value);
    }
}