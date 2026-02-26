// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic.aiida;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum AiidaSchema {
    SMART_METER_P1_RAW(Identifiers.SMART_METER_P1_RAW, ""),
    SMART_METER_P1_CIM_V1_04(Identifiers.SMART_METER_P1_CIM, CimVersions.V1_04),
    SMART_METER_P1_CIM_V1_12(Identifiers.SMART_METER_P1_CIM, CimVersions.V1_12),
    ACKNOWLEDGEMENT_CIM_V1_12(Identifiers.ACKNOWLEDGEMENT_CIM, CimVersions.V1_12),
    MIN_MAX_ENVELOPE_CIM_V1_12(Identifiers.MIN_MAX_ENVELOPE_CIM, CimVersions.V1_12);


    private final String identifier;
    private final String version;

    AiidaSchema(String identifier, String version) {
        this.identifier = identifier;
        this.version = version;
    }

    @JsonCreator
    public static Optional<AiidaSchema> forSchema(String schema) {
        return Arrays.stream(AiidaSchema.values())
                     .filter(aiidaSchema -> aiidaSchema.schema().equals(schema))
                     .findFirst();
    }

    public static Optional<AiidaSchema> forTopic(String topic) {
        var topicParts = Arrays.asList(topic.split("/"));
        var topicName = topicParts.getLast();
        return forTopicName(topicName);
    }

    public static Optional<AiidaSchema> forTopicName(String topicName) {
        return Arrays.stream(AiidaSchema.values())
                     .filter(aiidaSchema -> aiidaSchema.topicName().equals(topicName))
                     .findFirst();
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
        public static final String ACKNOWLEDGEMENT_CIM = "ACKNOWLEDGEMENT-CIM";
        public static final String MIN_MAX_ENVELOPE_CIM = "MIN-MAX-ENVELOPE-CIM";

        private Identifiers() {}
    }

    public static class CimVersions {
        public static final String V1_04 = "V1-04";
        public static final String V1_12 = "V1-12";

        private CimVersions() {}
    }
}
