// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.dataneeds.needs.aiida;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Locale;

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
                     .filter(op -> op.value().equals(value))
                     .findFirst()
                     .orElseThrow();
    }

    @JsonValue
    public String value() {
        return schema;
    }

    public String topicName() {
        return schema.toLowerCase(Locale.ROOT);
    }

    public String buildTopicPath(String baseTopic) {
        return String.join("/", baseTopic, topicName());
    }
}
