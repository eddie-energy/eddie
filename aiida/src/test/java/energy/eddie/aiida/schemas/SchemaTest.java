package energy.eddie.aiida.schemas;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.config.AiidaConfiguration;
import energy.eddie.aiida.errors.formatter.CimFormatterException;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.dataneed.AiidaLocalDataNeed;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.aiida.schemas.cim.v1_04.utils.CimUtil;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;
import energy.eddie.dataneeds.needs.aiida.AiidaSchema;
import nl.altindag.log.LogCaptor;
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
import static energy.eddie.aiida.utils.ObisCode.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchemaTest {
    private static final LogCaptor LOG_CAPTOR = LogCaptor.forClass(CimUtil.class);
    private static final Instant timestamp = Instant.ofEpochMilli(1729334059);
    private static final UUID aiidaId = UUID.fromString("3211ea05-d4ab-48ff-8613-8f4791a56606");
    private static final UUID dataSourceId = UUID.fromString("4211ea05-d4ab-48ff-8613-8f4791a56606");
    private static final UUID userId = UUID.fromString("5211ea05-d4ab-48ff-8613-8f4791a56606");
    private static final AiidaRecord aiidaRecordAT = new AiidaRecord(
            timestamp,
            AiidaAsset.SUBMETER,
            userId,
            dataSourceId,
            List.of(new AiidaRecordValue("1-0:1.7.0", POSITIVE_ACTIVE_INSTANTANEOUS_POWER, "10",
                                         KILO_WATT, "10",
                                         KILO_WATT),
                    new AiidaRecordValue("1-0:2.7.0", NEGATIVE_ACTIVE_INSTANTANEOUS_POWER, "50",
                                         KILO_WATT, "50",
                                         KILO_WATT),
                    new AiidaRecordValue("1-0:1.8.0", POSITIVE_ACTIVE_ENERGY, "50",
                                         KILO_WATT_HOUR, "50",
                                         KILO_WATT_HOUR),
                    new AiidaRecordValue("1-0:2.8.0", NEGATIVE_ACTIVE_ENERGY, "50",
                                         KILO_WATT_HOUR, "50",
                                         KILO_WATT_HOUR),
                    new AiidaRecordValue("1-0:31.7.0", INSTANTANEOUS_CURRENT_IN_PHASE_L1, "50",
                                         AMPERE, "50",
                                         AMPERE),
                    new AiidaRecordValue("1-0:51.7.0", INSTANTANEOUS_CURRENT_IN_PHASE_L2, "50",
                                         AMPERE, "50",
                                         AMPERE),
                    new AiidaRecordValue("1-0:71.7.0", INSTANTANEOUS_CURRENT_IN_PHASE_L3, "50",
                                         AMPERE, "50",
                                         AMPERE),
                    new AiidaRecordValue("1-0:13.7.0", INSTANTANEOUS_POWER_FACTOR, "5",
                                         NONE, "5",
                                         NONE),
                    new AiidaRecordValue("1-0:32.7.0", INSTANTANEOUS_VOLTAGE_IN_PHASE_L1, "10",
                                         VOLT, "10",
                                         VOLT),
                    new AiidaRecordValue("1-0:52.7.0", INSTANTANEOUS_VOLTAGE_IN_PHASE_L2, "10",
                                         VOLT, "10",
                                         VOLT),
                    new AiidaRecordValue("1-0:52.7.0", INSTANTANEOUS_VOLTAGE_IN_PHASE_L3, "10",
                                         VOLT, "10",
                                         VOLT),
                    new AiidaRecordValue("1-0:96.1.0", DEVICE_ID_1, "Device123",
                                         NONE, "Device123",
                                         NONE)
            )
    );
    private static final AiidaRecord aiidaRecordFR = new AiidaRecord(
            timestamp,
            AiidaAsset.SUBMETER,
            userId,
            dataSourceId,
            List.of(new AiidaRecordValue("PAPP",
                                         POSITIVE_ACTIVE_INSTANTANEOUS_POWER,
                                         "10",
                                         VOLT_AMPERE,
                                         "10",
                                         VOLT_AMPERE),
                    new AiidaRecordValue("BASE",
                                         POSITIVE_ACTIVE_ENERGY,
                                         "50",
                                         WATT_HOUR,
                                         "50",
                                         WATT_HOUR)
            )
    );
    private static final AiidaRecord aiidaRecordWithFaultyRecord = new AiidaRecord(
            timestamp,
            AiidaAsset.SUBMETER,
            userId,
            dataSourceId,
            List.of(new AiidaRecordValue("BASE",
                                         POSITIVE_ACTIVE_ENERGY,
                                         "ten",
                                         KILO_WATT_HOUR,
                                         "ten",
                                         KILO_WATT_HOUR)
            )
    );
    private static final AiidaRecord aiidaRecordWithUnsupportedQuantityType = new AiidaRecord(
            timestamp,
            AiidaAsset.SUBMETER,
            userId,
            dataSourceId,
            List.of(new AiidaRecordValue("PAPP",
                                         POSITIVE_REACTIVE_INSTANTANEOUS_POWER,
                                         "10",
                                         VOLT_AMPERE,
                                         "10",
                                         VOLT_AMPERE)
            )
    );

    @Mock
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new AiidaConfiguration().customObjectMapper().build();
    }

    @Test
    void schemaRaw() {
        var permissionMock = mock(Permission.class);

        var rawAiidaRecordAT = "{\"timestamp\":\"1970-01-21T00:22:14.059Z\",\"asset\":\"SUBMETER\",\"userId\":\"5211ea05-d4ab-48ff-8613-8f4791a56606\",\"dataSourceId\":\"4211ea05-d4ab-48ff-8613-8f4791a56606\",\"values\":[{\"rawTag\":\"1-0:1.7.0\",\"dataTag\":\"1-0:1.7.0\",\"rawValue\":\"10\",\"rawUnitOfMeasurement\":\"kW\",\"value\":\"10\",\"unitOfMeasurement\":\"kW\",\"sourceKey\":null},{\"rawTag\":\"1-0:2.7.0\",\"dataTag\":\"1-0:2.7.0\",\"rawValue\":\"50\",\"rawUnitOfMeasurement\":\"kW\",\"value\":\"50\",\"unitOfMeasurement\":\"kW\",\"sourceKey\":null},{\"rawTag\":\"1-0:1.8.0\",\"dataTag\":\"1-0:1.8.0\",\"rawValue\":\"50\",\"rawUnitOfMeasurement\":\"kWh\",\"value\":\"50\",\"unitOfMeasurement\":\"kWh\",\"sourceKey\":null},{\"rawTag\":\"1-0:2.8.0\",\"dataTag\":\"1-0:2.8.0\",\"rawValue\":\"50\",\"rawUnitOfMeasurement\":\"kWh\",\"value\":\"50\",\"unitOfMeasurement\":\"kWh\",\"sourceKey\":null},{\"rawTag\":\"1-0:31.7.0\",\"dataTag\":\"1-0:31.7.0\",\"rawValue\":\"50\",\"rawUnitOfMeasurement\":\"A\",\"value\":\"50\",\"unitOfMeasurement\":\"A\",\"sourceKey\":null},{\"rawTag\":\"1-0:51.7.0\",\"dataTag\":\"1-0:51.7.0\",\"rawValue\":\"50\",\"rawUnitOfMeasurement\":\"A\",\"value\":\"50\",\"unitOfMeasurement\":\"A\",\"sourceKey\":null},{\"rawTag\":\"1-0:71.7.0\",\"dataTag\":\"1-0:71.7.0\",\"rawValue\":\"50\",\"rawUnitOfMeasurement\":\"A\",\"value\":\"50\",\"unitOfMeasurement\":\"A\",\"sourceKey\":null},{\"rawTag\":\"1-0:13.7.0\",\"dataTag\":\"1-0:13.7.0\",\"rawValue\":\"5\",\"rawUnitOfMeasurement\":\"none\",\"value\":\"5\",\"unitOfMeasurement\":\"none\",\"sourceKey\":null},{\"rawTag\":\"1-0:32.7.0\",\"dataTag\":\"1-0:32.7.0\",\"rawValue\":\"10\",\"rawUnitOfMeasurement\":\"V\",\"value\":\"10\",\"unitOfMeasurement\":\"V\",\"sourceKey\":null},{\"rawTag\":\"1-0:52.7.0\",\"dataTag\":\"1-0:52.7.0\",\"rawValue\":\"10\",\"rawUnitOfMeasurement\":\"V\",\"value\":\"10\",\"unitOfMeasurement\":\"V\",\"sourceKey\":null},{\"rawTag\":\"1-0:52.7.0\",\"dataTag\":\"1-0:72.7.0\",\"rawValue\":\"10\",\"rawUnitOfMeasurement\":\"V\",\"value\":\"10\",\"unitOfMeasurement\":\"V\",\"sourceKey\":null},{\"rawTag\":\"1-0:96.1.0\",\"dataTag\":\"0-0:96.1.0\",\"rawValue\":\"Device123\",\"rawUnitOfMeasurement\":\"none\",\"value\":\"Device123\",\"unitOfMeasurement\":\"none\",\"sourceKey\":null}]}";
        var rawAiidaRecordFR = "{\"timestamp\":\"1970-01-21T00:22:14.059Z\",\"asset\":\"SUBMETER\",\"userId\":\"5211ea05-d4ab-48ff-8613-8f4791a56606\",\"dataSourceId\":\"4211ea05-d4ab-48ff-8613-8f4791a56606\",\"values\":[{\"rawTag\":\"PAPP\",\"dataTag\":\"1-0:1.7.0\",\"rawValue\":\"10\",\"rawUnitOfMeasurement\":\"VA\",\"value\":\"10\",\"unitOfMeasurement\":\"VA\",\"sourceKey\":null},{\"rawTag\":\"BASE\",\"dataTag\":\"1-0:1.8.0\",\"rawValue\":\"50\",\"rawUnitOfMeasurement\":\"Wh\",\"value\":\"50\",\"unitOfMeasurement\":\"Wh\",\"sourceKey\":null}]}";

        var rawFormatter = SchemaFormatter.getFormatter(aiidaId, AiidaSchema.SMART_METER_P1_RAW);
        var resultAT = rawFormatter.toSchema(aiidaRecordAT, objectMapper, permissionMock);
        var resultFR = rawFormatter.toSchema(aiidaRecordFR, objectMapper, permissionMock);

        assertEquals(rawAiidaRecordAT, new String(resultAT, StandardCharsets.UTF_8));
        assertEquals(rawAiidaRecordFR, new String(resultFR, StandardCharsets.UTF_8));
    }

    @Test
    void schemaCim() {
        LOG_CAPTOR.setLogLevelToTrace();

        var dataNeedId = UUID.fromString("1211ea05-d4ab-48ff-8613-8f4791a56606");
        var permissionId = UUID.fromString("2211ea05-d4ab-48ff-8613-8f4791a56606");

        var permissionMock = mock(Permission.class);
        var dataNeedMock = mock(AiidaLocalDataNeed.class);
        var dataSource = mock(DataSource.class);

        when(permissionMock.id()).thenReturn(permissionId);
        when(permissionMock.dataSource()).thenReturn(dataSource);
        when(permissionMock.dataNeed()).thenReturn(dataNeedMock);
        when(permissionMock.connectionId()).thenReturn("connectionId");

        when(dataNeedMock.dataNeedId()).thenReturn(dataNeedId);

        when(dataSource.countryCode()).thenReturn("AT");

        var cimFormatter = SchemaFormatter.getFormatter(aiidaId, AiidaSchema.SMART_METER_P1_CIM);
        assertDoesNotThrow(() -> cimFormatter.toSchema(aiidaRecordAT, objectMapper, permissionMock));
        assertThrows(CimFormatterException.class,
                     () -> cimFormatter.toSchema(aiidaRecordWithFaultyRecord, objectMapper, permissionMock));

        cimFormatter.toSchema(aiidaRecordWithUnsupportedQuantityType, objectMapper, permissionMock);
        assertThat(LOG_CAPTOR.getTraceLogs()).contains("AIIDA Record Value with data tag %s not supported.".formatted(
                POSITIVE_REACTIVE_INSTANTANEOUS_POWER));
    }
}
