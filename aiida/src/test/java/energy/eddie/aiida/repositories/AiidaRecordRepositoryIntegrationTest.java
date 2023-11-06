package energy.eddie.aiida.repositories;

import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordFactory;
import energy.eddie.aiida.models.record.IntegerAiidaRecord;
import energy.eddie.aiida.models.record.StringAiidaRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
// deactivate the default behaviour, instead use testcontainer
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create"
})
@Testcontainers
class AiidaRecordRepositoryIntegrationTest {
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> timescale = new PostgreSQLContainer<>(
            DockerImageName.parse("timescale/timescaledb:2.11.2-pg15")
                    .asCompatibleSubstituteFor("postgres")
    );

    @Autowired
    AiidaRecordRepository repository;

    @Test
    void givenIntegerAndStringRecord_valueIsDeserializedProperly() {
        Instant now = Instant.now();
        AiidaRecord intRecord = AiidaRecordFactory.createRecord("1.8.0", now, 237);
        AiidaRecord stringRecord = AiidaRecordFactory.createRecord("C.1.0", now, "Hello Test");

        repository.save(intRecord);
        repository.save(stringRecord);

        var all = repository.findAll();

        assertEquals(2, all.size());

        AiidaRecord first = all.get(0);
        assertTrue(first instanceof IntegerAiidaRecord);
        assertEquals("1.8.0", first.code());
        assertEquals(now.toEpochMilli(), first.timestamp().toEpochMilli());
        assertEquals(237, ((IntegerAiidaRecord) first).value());

        AiidaRecord second = all.get(1);
        assertTrue(second instanceof StringAiidaRecord);
        assertEquals("C.1.0", second.code());
        assertEquals(now.toEpochMilli(), second.timestamp().toEpochMilli());
        assertEquals("Hello Test", ((StringAiidaRecord) second).value());
    }
}