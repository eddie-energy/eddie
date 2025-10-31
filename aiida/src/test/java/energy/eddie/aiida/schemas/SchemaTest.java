// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.schemas;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.config.AiidaConfiguration;
import energy.eddie.aiida.errors.formatter.CimSchemaFormatterException;
import energy.eddie.aiida.errors.formatter.SchemaFormatterException;
import energy.eddie.aiida.errors.formatter.SchemaFormatterRegistryException;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.dataneed.AiidaLocalDataNeed;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.aiida.schemas.cim.BaseCimFormatterStrategy;
import energy.eddie.aiida.schemas.cim.v1_04.CimFormatter;
import energy.eddie.aiida.schemas.raw.RawFormatter;
import energy.eddie.aiida.services.ApplicationInformationService;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;
import energy.eddie.dataneeds.needs.aiida.AiidaSchema;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.test.json.JsonAssert;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static energy.eddie.api.agnostic.aiida.ObisCode.*;
import static energy.eddie.api.agnostic.aiida.UnitOfMeasurement.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchemaTest {
    private static final LogCaptor LOG_CAPTOR = LogCaptor.forClass(BaseCimFormatterStrategy.class);
    private static final Instant timestamp = Instant.ofEpochMilli(1729334059);
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

    private final ObjectMapper objectMapper = new AiidaConfiguration().customObjectMapper().build();

    @Mock
    private ApplicationInformationService applicationInformationService;

    @Mock
    private SchemaFormatterRegistry schemaFormatterRegistry;

    @BeforeEach
    void setUp() {
        var applicationInformation = mock(ApplicationInformation.class);
        when(applicationInformationService.applicationInformation()).thenReturn(applicationInformation);
        when(applicationInformation.aiidaId()).thenReturn(UUID.randomUUID());
    }

    @Test
    void schemaRaw() throws SchemaFormatterRegistryException, SchemaFormatterException {
        var permissionMock = mock(Permission.class);
        var rawFormatter = new RawFormatter(applicationInformationService, objectMapper);

        var rawAiidaRecordAT = "{\"timestamp\":\"1970-01-21T00:22:14.059Z\",\"asset\":\"SUBMETER\",\"userId\":\"5211ea05-d4ab-48ff-8613-8f4791a56606\",\"dataSourceId\":\"4211ea05-d4ab-48ff-8613-8f4791a56606\",\"values\":[{\"rawTag\":\"1-0:1.7.0\",\"dataTag\":\"1-0:1.7.0\",\"rawValue\":\"10\",\"rawUnitOfMeasurement\":\"kW\",\"value\":\"10\",\"unitOfMeasurement\":\"kW\"},{\"rawTag\":\"1-0:2.7.0\",\"dataTag\":\"1-0:2.7.0\",\"rawValue\":\"50\",\"rawUnitOfMeasurement\":\"kW\",\"value\":\"50\",\"unitOfMeasurement\":\"kW\"},{\"rawTag\":\"1-0:1.8.0\",\"dataTag\":\"1-0:1.8.0\",\"rawValue\":\"50\",\"rawUnitOfMeasurement\":\"kWh\",\"value\":\"50\",\"unitOfMeasurement\":\"kWh\"},{\"rawTag\":\"1-0:2.8.0\",\"dataTag\":\"1-0:2.8.0\",\"rawValue\":\"50\",\"rawUnitOfMeasurement\":\"kWh\",\"value\":\"50\",\"unitOfMeasurement\":\"kWh\"},{\"rawTag\":\"1-0:31.7.0\",\"dataTag\":\"1-0:31.7.0\",\"rawValue\":\"50\",\"rawUnitOfMeasurement\":\"A\",\"value\":\"50\",\"unitOfMeasurement\":\"A\"},{\"rawTag\":\"1-0:51.7.0\",\"dataTag\":\"1-0:51.7.0\",\"rawValue\":\"50\",\"rawUnitOfMeasurement\":\"A\",\"value\":\"50\",\"unitOfMeasurement\":\"A\"},{\"rawTag\":\"1-0:71.7.0\",\"dataTag\":\"1-0:71.7.0\",\"rawValue\":\"50\",\"rawUnitOfMeasurement\":\"A\",\"value\":\"50\",\"unitOfMeasurement\":\"A\"},{\"rawTag\":\"1-0:13.7.0\",\"dataTag\":\"1-0:13.7.0\",\"rawValue\":\"5\",\"rawUnitOfMeasurement\":\"none\",\"value\":\"5\",\"unitOfMeasurement\":\"none\"},{\"rawTag\":\"1-0:32.7.0\",\"dataTag\":\"1-0:32.7.0\",\"rawValue\":\"10\",\"rawUnitOfMeasurement\":\"V\",\"value\":\"10\",\"unitOfMeasurement\":\"V\"},{\"rawTag\":\"1-0:52.7.0\",\"dataTag\":\"1-0:52.7.0\",\"rawValue\":\"10\",\"rawUnitOfMeasurement\":\"V\",\"value\":\"10\",\"unitOfMeasurement\":\"V\"},{\"rawTag\":\"1-0:52.7.0\",\"dataTag\":\"1-0:72.7.0\",\"rawValue\":\"10\",\"rawUnitOfMeasurement\":\"V\",\"value\":\"10\",\"unitOfMeasurement\":\"V\"},{\"rawTag\":\"1-0:96.1.0\",\"dataTag\":\"0-0:96.1.0\",\"rawValue\":\"Device123\",\"rawUnitOfMeasurement\":\"none\",\"value\":\"Device123\",\"unitOfMeasurement\":\"none\"}]}";
        var rawAiidaRecordFR = "{\"timestamp\":\"1970-01-21T00:22:14.059Z\",\"asset\":\"SUBMETER\",\"userId\":\"5211ea05-d4ab-48ff-8613-8f4791a56606\",\"dataSourceId\":\"4211ea05-d4ab-48ff-8613-8f4791a56606\",\"values\":[{\"rawTag\":\"PAPP\",\"dataTag\":\"1-0:1.7.0\",\"rawValue\":\"10\",\"rawUnitOfMeasurement\":\"VA\",\"value\":\"10\",\"unitOfMeasurement\":\"VA\"},{\"rawTag\":\"BASE\",\"dataTag\":\"1-0:1.8.0\",\"rawValue\":\"50\",\"rawUnitOfMeasurement\":\"Wh\",\"value\":\"50\",\"unitOfMeasurement\":\"Wh\"}]}";

        when(schemaFormatterRegistry.formatterFor(AiidaSchema.SMART_METER_P1_RAW)).thenReturn(rawFormatter);

        var formatter = schemaFormatterRegistry.formatterFor(AiidaSchema.SMART_METER_P1_RAW);
        var resultAT = formatter.format(aiidaRecordAT, permissionMock);
        var resultFR = formatter.format(aiidaRecordFR, permissionMock);

        var comparator = JsonAssert.comparator(JSONCompareMode.LENIENT);
        comparator.assertIsMatch(rawAiidaRecordAT, new String(resultAT, StandardCharsets.UTF_8));
        comparator.assertIsMatch(rawAiidaRecordFR, new String(resultFR, StandardCharsets.UTF_8));
    }

    @Test
    void schemaCim_v1_04() throws SchemaFormatterException, SchemaFormatterRegistryException {
        LOG_CAPTOR.setLogLevelToTrace();

        var dataNeedId = UUID.fromString("1211ea05-d4ab-48ff-8613-8f4791a56606");
        var permissionId = UUID.fromString("2211ea05-d4ab-48ff-8613-8f4791a56606");
        var cimFormatter = new CimFormatter(applicationInformationService, objectMapper);

        var permissionMock = mock(Permission.class);
        var dataNeedMock = mock(AiidaLocalDataNeed.class);
        var dataSource = mock(DataSource.class);

        when(permissionMock.id()).thenReturn(permissionId);
        when(permissionMock.dataSource()).thenReturn(dataSource);
        when(permissionMock.dataNeed()).thenReturn(dataNeedMock);
        when(permissionMock.connectionId()).thenReturn("connectionId");

        when(dataNeedMock.dataNeedId()).thenReturn(dataNeedId);

        when(dataSource.countryCode()).thenReturn("AT");

        when(schemaFormatterRegistry.formatterFor(AiidaSchema.SMART_METER_P1_CIM)).thenReturn(cimFormatter);

        var formatter = schemaFormatterRegistry.formatterFor(AiidaSchema.SMART_METER_P1_CIM);
        assertDoesNotThrow(() -> formatter.format(aiidaRecordAT, permissionMock));
        assertThrows(CimSchemaFormatterException.class,
                     () -> formatter.format(aiidaRecordWithFaultyRecord, permissionMock));

        formatter.format(aiidaRecordWithUnsupportedQuantityType, permissionMock);
        assertThat(LOG_CAPTOR.getTraceLogs()).contains("AIIDA Record Value with data tag %s not supported.".formatted(
                POSITIVE_REACTIVE_INSTANTANEOUS_POWER));
    }
}
