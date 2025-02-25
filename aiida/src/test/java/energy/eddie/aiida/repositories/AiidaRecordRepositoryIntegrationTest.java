package energy.eddie.aiida.repositories;

import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.aiida.models.record.UnitOfMeasurement;
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

import static energy.eddie.aiida.utils.ObisCode.METER_SERIAL;
import static energy.eddie.aiida.utils.ObisCode.POSITIVE_ACTIVE_ENERGY;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
// deactivate the default behaviour, instead use testcontainer
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create"
})
@Testcontainers
class AiidaRecordRepositoryIntegrationTest {
    @SuppressWarnings("unused")
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> TIMESCALE = new PostgreSQLContainer<>(
            DockerImageName.parse("timescale/timescaledb:2.11.2-pg15")
                           .asCompatibleSubstituteFor("postgres")
    );
    private static final String dataSourceId = "4211ea05-d4ab-48ff-8613-8f4791a56606";
    @Autowired
    AiidaRecordRepository repository;

    @Test
    void givenIntegerAndStringRecord_valueIsDeserializedProperly() {
        Instant now = Instant.now();
        AiidaRecord intRecord = new AiidaRecord(now, "Test", dataSourceId, List.of(
                new AiidaRecordValue("1-0:1.8.0",
                                     POSITIVE_ACTIVE_ENERGY,
                                     "237",
                                     UnitOfMeasurement.KWH,
                                     "237",
                                     UnitOfMeasurement.KWH)));
        AiidaRecord stringRecord = new AiidaRecord(now, "Test", dataSourceId, List.of(
                new AiidaRecordValue("0-0:C.1.0",
                                     METER_SERIAL,
                                     "Hello Test",
                                     UnitOfMeasurement.NONE,
                                     "Hello Test",
                                     UnitOfMeasurement.NONE)));

        repository.save(intRecord);
        repository.save(stringRecord);

        var all = repository.findAll();

        assertEquals(2, all.size());

        AiidaRecord first = all.getFirst();
        assertEquals(POSITIVE_ACTIVE_ENERGY, first.aiidaRecordValue().getFirst().dataTag());
        assertEquals(now.toEpochMilli(), first.timestamp().toEpochMilli());
        assertEquals("237", first.aiidaRecordValue().getFirst().value());

        AiidaRecord second = all.get(1);
        assertEquals(METER_SERIAL, second.aiidaRecordValue().getFirst().dataTag());
        assertEquals(now.toEpochMilli(), second.timestamp().toEpochMilli());
        assertEquals("Hello Test", second.aiidaRecordValue().getFirst().value());
    }
}