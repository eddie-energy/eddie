// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.dataneeds.needs.aiida;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiidaSchemaTest {
    @Test
    void forSchema_returnsEnum_whenKnown() {
        assertEquals(AiidaSchema.SMART_METER_P1_RAW, AiidaSchema.forSchema("SMART-METER-P1-RAW"));
        assertEquals(AiidaSchema.SMART_METER_P1_CIM_V1_04, AiidaSchema.forSchema("SMART-METER-P1-CIM-V1-04"));
        assertEquals(AiidaSchema.SMART_METER_P1_CIM_V1_06, AiidaSchema.forSchema("SMART-METER-P1-CIM-V1-06"));
    }

    @Test
    void forSchema_throws_whenUnknown() {
        assertThrows(NoSuchElementException.class, () -> AiidaSchema.forSchema("UNKNOWN-SCHEMA"));
    }

    @Test
    void schema_and_topicName_and_buildTopicPath_areCorrect() {
        AiidaSchema s = AiidaSchema.SMART_METER_P1_RAW;
        assertEquals("SMART-METER-P1-RAW", s.schema());
        assertEquals("smart-meter-p1-raw", s.topicName());
        assertEquals("base/smart-meter-p1-raw", s.buildTopicPath("base"));
    }

    @Test
    void json_serialization_and_deserialization_respectAnnotations() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(AiidaSchema.SMART_METER_P1_CIM_V1_04);
        assertEquals("\"SMART-METER-P1-CIM-V1-04\"", json);

        AiidaSchema deserialized = mapper.readValue(json, AiidaSchema.class);
        assertEquals(AiidaSchema.SMART_METER_P1_CIM_V1_04, deserialized);
    }
}
