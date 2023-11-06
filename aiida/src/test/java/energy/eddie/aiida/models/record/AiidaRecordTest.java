package energy.eddie.aiida.models.record;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

class AiidaRecordTest {
    @Test
    void testJpaConstructorsForCoverage() {
        var record1 = new IntegerAiidaRecord();
        var record2 = new StringAiidaRecord();

        assertNull(record1.code);

        assertNull(record2.timestamp);
    }
}
