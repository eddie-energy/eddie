package energy.eddie.aiida.models.record;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AiidaRecordFactoryTest {
    @Test
    void givenObisCode_testCorrectSubclassMapping() {
        Instant now = Instant.now();
        AiidaRecord intRecord_1_7_0 = AiidaRecordFactory.createRecord("1.7.0", now, 89);
        AiidaRecord intRecord_2_7_0 = AiidaRecordFactory.createRecord("2.7.0", now, 64);
        AiidaRecord intRecord_1_8_0 = AiidaRecordFactory.createRecord("1.8.0", now, 1412);
        AiidaRecord intRecord_2_8_0 = AiidaRecordFactory.createRecord("2.8.0", now, 112);
        AiidaRecord stringRecord = AiidaRecordFactory.createRecord("C.1.0", now, "FooBar");


        assertThat(intRecord_1_7_0, instanceOf(IntegerAiidaRecord.class));
        assertThat(intRecord_1_8_0, instanceOf(IntegerAiidaRecord.class));
        assertThat(intRecord_2_7_0, instanceOf(IntegerAiidaRecord.class));
        assertThat(intRecord_2_8_0, instanceOf(IntegerAiidaRecord.class));
        assertThat(stringRecord, instanceOf(StringAiidaRecord.class));

        var castedIntRecord = (IntegerAiidaRecord) intRecord_2_7_0;
        assertEquals("2.7.0", castedIntRecord.code());
        assertEquals(now, castedIntRecord.timestamp());
        assertEquals(64, castedIntRecord.value());

        var castedStringRecord = (StringAiidaRecord) stringRecord;
        assertEquals("C.1.0", castedStringRecord.code());
        assertEquals("FooBar", castedStringRecord.value());
        assertEquals(now, castedStringRecord.timestamp());
    }

    @Test
    void givenInvalidObisCode_throws() {
        Instant now = Instant.now();

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
                AiidaRecordFactory.createRecord("X.Y.Z", now, "INVALID_BBB"));

        assertEquals("No definition which AiidaRecord subclass should be used for OBIS code X.Y.Z", thrown.getMessage());
    }

    @Test
    void givenInvalidValue_createRecord_throws() {
        Instant now = Instant.now();

        assertThrows(IllegalArgumentException.class, () -> AiidaRecordFactory.createRecord("1.7.0", now, "INVALID"));
        assertThrows(IllegalArgumentException.class, () -> AiidaRecordFactory.createRecord("C.1.0", now, 53L));
    }

    @Test
    void givenValidCode_jsonMapping_asExpected() throws JsonProcessingException {
        var mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        var begin = Instant.parse("2023-10-01T10:00:00.00Z");

        var integerRecord = AiidaRecordFactory.createRecord("1.7.0", begin, 372);
        var integerRecordExpected = "{\"type\":\"IntegerAiidaRecord\",\"timestamp\":1696154400.000000000,\"code\":\"1.7.0\",\"value\":372}";
        var stringRecord = AiidaRecordFactory.createRecord("C.1.0", begin.plusSeconds(10), "Hello test world");
        var stringRecordExpected = "{\"type\":\"StringAiidaRecord\",\"timestamp\":1696154410.000000000,\"code\":\"C.1.0\",\"value\":\"Hello test world\"}";

        var integerJson = mapper.writeValueAsString(integerRecord);
        var stringJson = mapper.writeValueAsString(stringRecord);

        assertEquals(integerRecordExpected, integerJson);
        assertEquals(stringRecordExpected, stringJson);
    }

    @Test
    void givenObisCodeThatHasNoImplementation_throws() throws NoSuchFieldException, IllegalAccessException {
        var now = Instant.now();
        Field field = AiidaRecordFactory.class.getDeclaredField("RECORD_MAP");
        field.setAccessible(true);
        Map<String, Class<? extends AiidaRecord>> recordMap = (Map<String, Class<? extends AiidaRecord>>) field.get(null);

        recordMap.put("T.E.S", FailingTestAiidaRecord.class);


        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
                AiidaRecordFactory.createRecord("T.E.S", now, "Some_Value"));

        assertThat(thrown.getMessage(), startsWith("No implementation that creates a "));
    }

    static class FailingTestAiidaRecord extends AiidaRecord {
        protected FailingTestAiidaRecord(Instant timestamp, String code) {
            super(timestamp, code);
        }
    }
}
