package energy.eddie.aiida.repositories;

import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
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
import java.util.List;

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
        AiidaRecord intRecord = new AiidaRecord(now, "Test", List.of(
                new AiidaRecordValue("1.8.0", "1.8.0", "237", "kWh", "237", "kWh")));
        AiidaRecord stringRecord = new AiidaRecord(now, "Test", List.of(
                new AiidaRecordValue("C.1.0", "C.1.0", "Hello Test", "text", "Hello Test", "text")));

        repository.save(intRecord);
        repository.save(stringRecord);

        var all = repository.findAll();

        assertEquals(2, all.size());

        AiidaRecord first = all.get(0);
        assertEquals("1.8.0", first.aiidaRecordValue().getFirst().dataTag());
        assertEquals(now.toEpochMilli(), first.timestamp().toEpochMilli());
        assertEquals("237", first.aiidaRecordValue().getFirst().value());

        AiidaRecord second = all.get(1);
        assertEquals("C.1.0", second.aiidaRecordValue().getFirst().dataTag());
        assertEquals(now.toEpochMilli(), second.timestamp().toEpochMilli());
        assertEquals("Hello Test", second.aiidaRecordValue().getFirst().value());
    }
}