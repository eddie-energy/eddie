package energy.eddie.aiida.repositories;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.adapters.datasource.SmartMeterAdapterMeasurement;
import energy.eddie.aiida.adapters.datasource.fr.transformer.standard.MicroTeleinfoV3AdapterStandardModeMeasurement;
import energy.eddie.aiida.adapters.datasource.fr.transformer.standard.MicroTeleinfoV3StandardModeJson;
import energy.eddie.aiida.config.AiidaConfiguration;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.aiida.models.record.UnitOfMeasurement;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;
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
import java.util.UUID;

import static energy.eddie.aiida.utils.ObisCode.METER_SERIAL;
import static energy.eddie.aiida.utils.ObisCode.POSITIVE_ACTIVE_ENERGY;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
// deactivate the default behaviour, instead use testcontainer
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {"spring.jpa.hibernate.ddl-auto=create"})
@Testcontainers
class AiidaRecordRepositoryIntegrationTest {
    @SuppressWarnings("unused")
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> TIMESCALE = new PostgreSQLContainer<>(DockerImageName.parse(
            "timescale/timescaledb:2.11.2-pg15").asCompatibleSubstituteFor("postgres"));
    private static final UUID dataSourceId = UUID.fromString("4211ea05-d4ab-48ff-8613-8f4791a56606");
    private static final UUID userId = UUID.fromString("5211ea05-d4ab-48ff-8613-8f4791a56606");
    private final ObjectMapper objectMapper = new AiidaConfiguration().customObjectMapper().build();
    @Autowired
    private AiidaRecordRepository repository;

    @Test
    void givenIntegerAndStringRecord_valueIsDeserializedProperly() {
        Instant now = Instant.now();
        AiidaRecord intRecord = new AiidaRecord(now, AiidaAsset.SUBMETER, userId, dataSourceId, List.of(
                new AiidaRecordValue("1-0:1.8.0",
                                     POSITIVE_ACTIVE_ENERGY,
                                     "237",
                                     UnitOfMeasurement.KILO_WATT_HOUR,
                                     "237",
                                     UnitOfMeasurement.KILO_WATT_HOUR)));
        AiidaRecord stringRecord = new AiidaRecord(now, AiidaAsset.SUBMETER, userId, dataSourceId, List.of(
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
        assertEquals(POSITIVE_ACTIVE_ENERGY, first.aiidaRecordValues().getFirst().dataTag());
        assertEquals(now.toEpochMilli(), first.timestamp().toEpochMilli());
        assertEquals("237", first.aiidaRecordValues().getFirst().value());

        AiidaRecord second = all.get(1);
        assertEquals(METER_SERIAL, second.aiidaRecordValues().getFirst().dataTag());
        assertEquals(now.toEpochMilli(), second.timestamp().toEpochMilli());
        assertEquals("Hello Test", second.aiidaRecordValues().getFirst().value());
    }

    @Test
    void givenMicroTeleinfoStandardModeRecord_sanitizeNullValue_persistsProperly() throws JsonProcessingException {
        var standardModeJson = """
                {
                  "ADSC": {
                    "raw": "841875104423",
                    "value": 841875104423
                  },
                  "VTIC": {
                    "raw": "02",
                    "value": 2
                  },
                  "DATE": {
                    "raw": "",
                    "value": "",
                    "timestamp": {
                      "dst": "summer",
                      "date": "2025-04-14T12:54:38.000Z"
                    }
                  },
                  "NGTF": {
                    "raw": "      BASE      ",
                    "value": "      BASE      "
                  },
                  "LTARF": {
                    "raw": "      BASE      ",
                    "value": "      BASE      "
                  },
                  "EAST": {
                    "raw": "032507388",
                    "value": 32507388
                  },
                  "EASF01": {
                    "raw": "032507388",
                    "value": 32507388
                  },
                  "EASF03": {
                    "raw": "000000000",
                    "value": 0
                  },
                  "EASF04": {
                    "raw": "000000000",
                    "value": 0
                  },
                  "EASF05": {
                    "raw": "000000000",
                    "value": 0
                  },
                  "EASF06": {
                    "raw": "000000000",
                    "value": 0
                  },
                  "EASF07": {
                    "raw": "000000000",
                    "value": 0
                  },
                  "EASF08": {
                    "raw": "000000000",
                    "value": 0
                  },
                  "EASF09": {
                    "raw": "000000000",
                    "value": 0
                  },
                  "EASF10": {
                    "raw": "000000000",
                    "value": 0
                  },
                  "EASD01": {
                    "raw": "007545288",
                    "value": 7545288
                  },
                  "EASD02": {
                    "raw": "006699887",
                    "value": 6699887
                  },
                  "EASD04": {
                    "raw": "011806842",
                    "value": 11806842
                  },
                  "EAIT": {
                    "raw": "009509008",
                    "value": 9509008
                  },
                  "ERQ1": {
                    "raw": "003136793",
                    "value": 3136793
                  },
                  "ERQ2": {
                    "raw": "000015124",
                    "value": 15124
                  },
                  "ERQ3": {
                    "raw": "001432483",
                    "value": 1432483
                  },
                  "ERQ4": {
                    "raw": "006198829",
                    "value": 6198829
                  },
                  "IRMS1": {
                    "raw": "005",
                    "value": 5
                  },
                  "URMS1": {
                    "raw": "236",
                    "value": 236
                  },
                  "PREF": {
                    "raw": "12",
                    "value": 12
                  },
                  "PCOUP": {
                    "raw": "12",
                    "value": 12
                  },
                  "SINSTS": {
                    "raw": "00000",
                    "value": 0
                  },
                  "SMAXSN-1": {
                    "raw": "07000",
                    "value": 7000,
                    "timestamp": {
                      "dst": "summer",
                      "date": "2025-04-08T06:11:26.000Z"
                    }
                  },
                  "SINSTI": {
                    "raw": "01156",
                    "value": 1156
                  },
                  "SMAXIN": {
                    "raw": "05030",
                    "value": 5030,
                    "timestamp": {
                      "dst": "summer",
                      "date": "2025-04-14T12:45:12.000Z"
                    }
                  },
                  "SMAXIN-1": {
                    "raw": "04140",
                    "value": 4140,
                    "timestamp": {
                      "dst": "summer",
                      "date": "2025-04-08T12:53:25.000Z"
                    }
                  },
                  "CCASN": {
                    "raw": "00000",
                    "value": 0,
                    "timestamp": {
                      "dst": "summer",
                      "date": "2025-04-14T12:30:00.000Z"
                    }
                  },
                  "CCASN-1": {
                    "raw": "00000",
                    "value": 0,
                    "timestamp": {
                      "dst": "summer",
                      "date": "2025-04-14T12:00:00.000Z"
                    }
                  },
                  "CCAIN": {
                    "raw": "02556",
                    "value": 2556,
                    "timestamp": {
                      "dst": "summer",
                      "date": "2025-04-14T12:30:00.000Z"
                    }
                  },
                  "CCAIN-1": {
                    "raw": "03582",
                    "value": 3582,
                    "timestamp": {
                      "dst": "summer",
                      "date": "2025-04-14T12:00:00.000Z"
                    }
                  },
                  "UMOY1": {
                    "raw": "237",
                    "value": 237,
                    "timestamp": {
                      "dst": "summer",
                      "date": "2025-04-14T12:50:00.000Z"
                    }
                  },
                  "STGE": {
                    "raw": "003A4301",
                    "value": "003A4301"
                  },
                  "MSG1": {
                    "raw": "PAS DE          MESSAGE     \\u0000   ",
                    "value": "PAS DE          MESSAGE     \\u0000   "
                  },
                  "PRM": {
                    "raw": "06444138907938",
                    "value": 6444138907938
                  },
                  "RELAIS": {
                    "raw": "000",
                    "value": 0
                  },
                  "NTARF": {
                    "raw": "01",
                    "value": 1
                  },
                  "NJOURF": {
                    "raw": "00",
                    "value": 0
                  },
                  "NJOURF+1": {
                    "raw": "00",
                    "value": 0
                  }
                }
                """;

        var standardModeMeasurement = objectMapper.readValue(standardModeJson, MicroTeleinfoV3StandardModeJson.class);
        var aiidaRecordValues = standardModeMeasurement.energyData()
                                                       .entrySet()
                                                       .stream()
                                                       .map(entry -> new MicroTeleinfoV3AdapterStandardModeMeasurement(
                                                               entry.getKey(),
                                                               String.valueOf(entry.getValue()
                                                                                   .sanitizedValue(entry.getKey()))))
                                                       .map(SmartMeterAdapterMeasurement::toAiidaRecordValue)
                                                       .toList();

        var timestamp = Instant.now();
        var aiidaRecord = new AiidaRecord(timestamp,
                                          AiidaAsset.CONNECTION_AGREEMENT_POINT,
                                          UUID.randomUUID(),
                                          UUID.randomUUID(),
                                          aiidaRecordValues);

        repository.save(aiidaRecord);

        assertEquals(1, repository.count());
        assertEquals("PAS DE          MESSAGE        ",
                     repository.findAll()
                               .stream()
                               .findFirst()
                               .orElseThrow()
                               .aiidaRecordValues()
                               .stream()
                               .filter(aiidaRecordValue -> aiidaRecordValue.rawTag().equals("MSG1"))
                               .findFirst()
                               .orElseThrow()
                               .rawValue());
    }
}