package energy.eddie.aiida.models.record;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
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


        // no code with a double value available at this time
        // AiidaRecord doubleRecord = AiidaRecordFactoryTest.createRecord("1.7.0", Instant.now(), 10);

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

        assertThrows(IllegalArgumentException.class, () -> AiidaRecordFactory.createRecord("X.Y.Z", now, "INVALID_BBB"));
    }

    @Test
    void givenInvalidValue_createRecord_throws() {
        Instant now = Instant.now();

        assertThrows(IllegalArgumentException.class, () -> AiidaRecordFactory.createRecord("1.7.0", now, "INVALID"));
        assertThrows(IllegalArgumentException.class, () -> AiidaRecordFactory.createRecord("C.1.0", now, 53L));
    }
}
