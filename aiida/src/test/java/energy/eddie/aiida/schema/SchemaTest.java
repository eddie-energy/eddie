package energy.eddie.aiida.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.config.AiidaConfiguration;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.aiida.schemas.SchemaFormatter;
import energy.eddie.dataneeds.validation.schema.AiidaSchema;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class SchemaTest {
    @Mock
    private ObjectMapper objectMapper;

    private static final Instant timestamp = Instant.ofEpochMilli(1729334059);

    private static final AiidaRecord aiidaRecordAT = new AiidaRecord(
            timestamp,
            "AT",
            List.of(new AiidaRecordValue("1-0:1.7.0", "1-0:1.7.0", "10", "kW", "10", "kW"),
                    new AiidaRecordValue("1-0:1.8.0", "1-0:1.8.0", "50", "kWh", "50", "kWh")));

    private static final AiidaRecord aiidaRecordFR = new AiidaRecord(
            timestamp,
            "FR",
            List.of(new AiidaRecordValue("PAPP", "1-0:1.7.0", "10", "VA", "10", "VA"),
                    new AiidaRecordValue("BASE", "1-0:1.8.0", "50", "Wh", "50", "Wh")));

    @BeforeEach
    void setUp() {
        objectMapper = new AiidaConfiguration().objectMapper();
    }

    @Test
    void schemaRaw() {
        var rawAiidaRecordAT = "{\"timestamp\":1729334.059000000,\"asset\":\"AT\",\"values\":[{\"rawTag\":\"1-0:1.7.0\",\"dataTag\":\"1-0:1.7.0\",\"rawValue\":\"10\",\"rawUnitOfMeasurement\":\"kW\",\"value\":\"10\",\"unitOfMeasurement\":\"kW\"},{\"rawTag\":\"1-0:1.8.0\",\"dataTag\":\"1-0:1.8.0\",\"rawValue\":\"50\",\"rawUnitOfMeasurement\":\"kWh\",\"value\":\"50\",\"unitOfMeasurement\":\"kWh\"}]}";
        var rawAiidaRecordFR = "{\"timestamp\":1729334.059000000,\"asset\":\"FR\",\"values\":[{\"rawTag\":\"PAPP\",\"dataTag\":\"1-0:1.7.0\",\"rawValue\":\"10\",\"rawUnitOfMeasurement\":\"VA\",\"value\":\"10\",\"unitOfMeasurement\":\"VA\"},{\"rawTag\":\"BASE\",\"dataTag\":\"1-0:1.8.0\",\"rawValue\":\"50\",\"rawUnitOfMeasurement\":\"Wh\",\"value\":\"50\",\"unitOfMeasurement\":\"Wh\"}]}";

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
