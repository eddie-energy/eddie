package energy.eddie.aiida.datasources.fr;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import energy.eddie.aiida.models.record.UnitOfMeasurement;
import energy.eddie.aiida.utils.ObisCode;
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

        String raw = node.get("raw").asText();
        JsonNode valueNode = node.get("value");
        Object value = valueNode.asText();

        UnitOfMeasurement unit = determineUnit(jp.getCurrentName());
        ObisCode obisCode = determineObisCode(jp.getCurrentName());

        return new MicroTeleinfoV3Json.TeleinfoDataField(raw, value, unit, obisCode);
    }

    private UnitOfMeasurement determineUnit(String fieldName) {
        return switch (fieldName) {
            case "ISOUSC", "IINST", "IMAX" -> UnitOfMeasurement.AMPERE;
            case "BASE" -> UnitOfMeasurement.WH;
            case "PAPP" -> UnitOfMeasurement.VOLTAMPERE;
            default -> UnitOfMeasurement.UNKNOWN;
        };
    }

    private ObisCode determineObisCode(String fieldName) {
        return switch (fieldName) {
            case "BASE" -> ObisCode.POSITIVE_ACTIVE_ENERGY;
            case "PAPP" -> ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER;
            default -> ObisCode.UNKNOWN;
        };
    }
}