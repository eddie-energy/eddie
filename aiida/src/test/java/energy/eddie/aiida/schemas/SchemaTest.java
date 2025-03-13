package energy.eddie.aiida.schemas;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.config.AiidaConfiguration;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;
import energy.eddie.dataneeds.needs.aiida.AiidaSchema;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static energy.eddie.aiida.models.record.UnitOfMeasurement.*;
import static energy.eddie.aiida.utils.ObisCode.POSITIVE_ACTIVE_ENERGY;
import static energy.eddie.aiida.utils.ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class SchemaTest {
    @Mock
    private ObjectMapper objectMapper;

    private static final Instant timestamp = Instant.ofEpochMilli(1729334059);
    private static final UUID dataSourceId = UUID.fromString("4211ea05-d4ab-48ff-8613-8f4791a56606");
    private static final UUID userId = UUID.fromString("5211ea05-d4ab-48ff-8613-8f4791a56606");

    private static final AiidaRecord aiidaRecordAT = new AiidaRecord(
            timestamp,
            AiidaAsset.SUBMETER,
            userId,
            dataSourceId,
            List.of(new AiidaRecordValue("1-0:1.7.0", POSITIVE_ACTIVE_INSTANTANEOUS_POWER, "10", KW, "10", KW),
                    new AiidaRecordValue("1-0:1.8.0", POSITIVE_ACTIVE_ENERGY, "50", KWH, "50", KWH)));

    private static final AiidaRecord aiidaRecordFR = new AiidaRecord(
            timestamp,
            AiidaAsset.SUBMETER,
            userId,
            dataSourceId,
            List.of(new AiidaRecordValue("PAPP",
                                         POSITIVE_ACTIVE_INSTANTANEOUS_POWER,
                                         "10",
                                         VOLTAMPERE,
                                         "10",
                                         VOLTAMPERE),
                    new AiidaRecordValue("BASE", POSITIVE_ACTIVE_ENERGY, "50", WH, "50", WH)));

    @BeforeEach
    void setUp() {
        objectMapper = new AiidaConfiguration().objectMapper();
    }

    @Test
    void schemaRaw() {
        var rawAiidaRecordAT = "{\"timestamp\":\"1970-01-21T00:22:14.059Z\",\"asset\":\"SUBMETER\",\"userId\":\"5211ea05-d4ab-48ff-8613-8f4791a56606\",\"dataSourceId\":\"4211ea05-d4ab-48ff-8613-8f4791a56606\",\"values\":[{\"rawTag\":\"1-0:1.7.0\",\"dataTag\":\"1-0:1.7.0\",\"rawValue\":\"10\",\"rawUnitOfMeasurement\":\"kW\",\"value\":\"10\",\"unitOfMeasurement\":\"kW\"},{\"rawTag\":\"1-0:1.8.0\",\"dataTag\":\"1-0:1.8.0\",\"rawValue\":\"50\",\"rawUnitOfMeasurement\":\"kWh\",\"value\":\"50\",\"unitOfMeasurement\":\"kWh\"}]}";
        var rawAiidaRecordFR = "{\"timestamp\":\"1970-01-21T00:22:14.059Z\",\"asset\":\"SUBMETER\",\"userId\":\"5211ea05-d4ab-48ff-8613-8f4791a56606\",\"dataSourceId\":\"4211ea05-d4ab-48ff-8613-8f4791a56606\",\"values\":[{\"rawTag\":\"PAPP\",\"dataTag\":\"1-0:1.7.0\",\"rawValue\":\"10\",\"rawUnitOfMeasurement\":\"VA\",\"value\":\"10\",\"unitOfMeasurement\":\"VA\"},{\"rawTag\":\"BASE\",\"dataTag\":\"1-0:1.8.0\",\"rawValue\":\"50\",\"rawUnitOfMeasurement\":\"Wh\",\"value\":\"50\",\"unitOfMeasurement\":\"Wh\"}]}";

        var rawFormatter = SchemaFormatter.getFormatter(AiidaSchema.SMART_METER_P1_RAW);
        var resultAT = rawFormatter.toSchema(aiidaRecordAT, objectMapper);
        var resultFR = rawFormatter.toSchema(aiidaRecordFR, objectMapper);

        assertEquals(rawAiidaRecordAT, new String(resultAT, StandardCharsets.UTF_8));
        assertEquals(rawAiidaRecordFR, new String(resultFR, StandardCharsets.UTF_8));
    }

    @Test
    void schemaCim() {
        var cimFormatter = SchemaFormatter.getFormatter(AiidaSchema.SMART_METER_P1_CIM);
        assertThrows(NotImplementedException.class, () -> cimFormatter.toSchema(aiidaRecordAT, objectMapper));
    }
}
