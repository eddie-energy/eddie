// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic.aiida;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiidaSchemaTest {
    @Test
    void schema_addsVersion_whenCim() {
        // Given
        var schema = AiidaSchema.SMART_METER_P1_CIM_V1_12;

        // When
        var schemaValue = schema.schema();

        // Then
        assertEquals("SMART-METER-P1-CIM-V1-12", schemaValue);
    }

    @Test
    void schema_doesNotAddVersion_whenRaw() {
        // Given
        var schema = AiidaSchema.SMART_METER_P1_RAW;

        // When
        var schemaValue = schema.schema();

        // Then
        assertEquals("SMART-METER-P1-RAW", schemaValue);
    }

    @Test
    void forSchema_returnsCimSchemaWithVersion_whenCim() {
        // Given
        var schemaValue = "SMART-METER-P1-CIM-V1-12";

        // When
        var schema = AiidaSchema.forSchema(schemaValue);

        // Then
        assertTrue(schema.isPresent());
        assertEquals(AiidaSchema.SMART_METER_P1_CIM_V1_12, schema.get());
    }

    @Test
    void forSchema_returnsRawWithoutVersion_whenRaw() {
        // Given
        var schemaValue = "SMART-METER-P1-RAW";

        // When
        var schema = AiidaSchema.forSchema(schemaValue);

        // Then
        assertTrue(schema.isPresent());
        assertEquals(AiidaSchema.SMART_METER_P1_RAW, schema.get());
    }

    @Test
    void forSchema_returnsEmpty_whenInvalidSchema() {
        // Given
        var schemaValue = "INVALID-SCHEMA";

        // When
        var schema = AiidaSchema.forSchema(schemaValue);

        // Then
        assertTrue(schema.isEmpty());
    }

    @Test
    void forTopic_returnsSchema_whenCim() {
        // Given
        var topic = "base/path/smart-meter-p1-cim-v1-12";

        // When
        var schema = AiidaSchema.forTopic(topic);

        // Then
        assertTrue(schema.isPresent());
        assertEquals(AiidaSchema.SMART_METER_P1_CIM_V1_12, schema.get());
    }

    @Test
    void forTopic_returnsEmpty_whenInvalidTopic() {
        // Given
        var topic = "base/path/invalid-topic";

        // When
        var schema = AiidaSchema.forTopic(topic);

        // Then
        assertTrue(schema.isEmpty());
    }

    @Test
    void forTopicName_returnsSchema_whenCim() {
        // Given
        var topicName = "smart-meter-p1-cim-v1-12";

        // When
        var schema = AiidaSchema.forTopicName(topicName);

        // Then
        assertTrue(schema.isPresent());
        assertEquals(AiidaSchema.SMART_METER_P1_CIM_V1_12, schema.get());
    }

    @Test
    void forTopicName_returnsEmpty_whenInvalidTopicName() {
        // Given
        var topicName = "invalid-topic-name";

        // When
        var schema = AiidaSchema.forTopicName(topicName);

        // Then
        assertTrue(schema.isEmpty());
    }

    @Test
    void topicName_returnsLowerCaseSchema() {
        // Given
        var schema = AiidaSchema.SMART_METER_P1_CIM_V1_12;

        // When
        var topicName = schema.topicName();

        // Then
        assertEquals("smart-meter-p1-cim-v1-12", topicName);
    }

    @Test
    void buildTopicPath_appendTopicNameToBasePath() {
        // Given
        var schema = AiidaSchema.SMART_METER_P1_RAW;
        var basePath = "base/path";

        // When
        var topicPath = schema.buildTopicPath(basePath);

        // Then
        assertEquals("base/path/smart-meter-p1-raw", topicPath);
    }
}
