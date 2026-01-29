package energy.eddie.dataneeds.needs.aiida;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiidaSchemaTest {
    @Test
    void forValue_returnsEnum_whenKnown() {
        assertEquals(AiidaSchema.SMART_METER_P1_RAW, AiidaSchema.forValue("SMART-METER-P1-RAW"));
        assertEquals(AiidaSchema.SMART_METER_P1_CIM, AiidaSchema.forValue("SMART-METER-P1-CIM"));
    }

    @Test
    void forValue_throws_whenUnknown() {
        assertThrows(NoSuchElementException.class, () -> AiidaSchema.forValue("UNKNOWN-SCHEMA"));
    }

    @Test
    void value_and_topicName_and_buildTopicPath_areCorrect() {
        AiidaSchema s = AiidaSchema.SMART_METER_P1_RAW;
        assertEquals("SMART-METER-P1-RAW", s.value());
        assertEquals("smart-meter-p1-raw", s.topicName());
        assertEquals("base/smart-meter-p1-raw", s.buildTopicPath("base"));
    }

    @Test
    void json_serialization_and_deserialization_respectAnnotations() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(AiidaSchema.SMART_METER_P1_CIM);
        assertEquals("\"SMART-METER-P1-CIM\"", json);

        AiidaSchema deserialized = mapper.readValue(json, AiidaSchema.class);
        assertEquals(AiidaSchema.SMART_METER_P1_CIM, deserialized);
    }
}
