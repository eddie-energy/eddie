package energy.eddie.dataneeds.needs.aiida;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum AiidaSchema {
    SMART_METER_P1_RAW("SMART-METER-P1-RAW"),
    SMART_METER_P1_CIM("SMART-METER-P1-CIM");

    private final String schema;

    AiidaSchema(String schema) {
        this.schema = schema;
    }

    @JsonCreator
    public static AiidaSchema forValue(String value) {
        return Arrays.stream(AiidaSchema.values())
                     .filter(op -> op.getValue().equals(value))
                     .findFirst()
                     .orElseThrow();
    }

    @JsonValue
    public String getValue() {
        return schema;
    }
}
