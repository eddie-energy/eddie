package energy.eddie.api.agnostic.aiida;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Locale;

public enum AiidaSchema {
    SMART_METER_P1_RAW(Identifiers.SMART_METER_P1_RAW, ""),
    SMART_METER_P1_CIM_V1_04(Identifiers.SMART_METER_P1_CIM, "V1-04"),
    SMART_METER_P1_CIM_V1_12(Identifiers.SMART_METER_P1_CIM, "V1-12");

    private final String identifier;
    private final String version;

    AiidaSchema(String identifier, String version) {
        this.identifier = identifier;
        this.version = version;
    }

    @JsonCreator
    public static AiidaSchema forSchema(String schema) {
        return Arrays.stream(AiidaSchema.values())
                     .filter(aiidaSchema -> aiidaSchema.schema().equals(schema))
                     .findFirst()
                     .orElseThrow();
    }

    @JsonValue
    public String schema() {
        return version.isEmpty() ? identifier : identifier + "-" + version;
    }

    public String version() {
        return version;
    }

    public String topicName() {
        return schema().toLowerCase(Locale.ROOT);
    }

    public String buildTopicPath(String baseTopic) {
        return String.join("/", baseTopic, topicName());
    }

    public static class Identifiers {
        public static final String SMART_METER_P1_RAW = "SMART-METER-P1-RAW";
        public static final String SMART_METER_P1_CIM = "SMART-METER-P1-CIM";

        private Identifiers() {}
    }
}
