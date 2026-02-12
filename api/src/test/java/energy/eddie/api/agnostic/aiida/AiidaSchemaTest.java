// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic.aiida;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        assertEquals(AiidaSchema.SMART_METER_P1_CIM_V1_12, schema);
    }

    @Test
    void forSchema_returnsRawWithoutVersion_whenRaw() {
        // Given
        var schemaValue = "SMART-METER-P1-RAW";

        // When
        var schema = AiidaSchema.forSchema(schemaValue);

        // Then
        assertEquals(AiidaSchema.SMART_METER_P1_RAW, schema);
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
