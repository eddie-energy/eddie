// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.schemas;

import energy.eddie.aiida.application.information.ApplicationInformation;
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
import energy.eddie.api.agnostic.aiida.AiidaAsset;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static energy.eddie.api.agnostic.aiida.ObisCode.*;
import static energy.eddie.api.agnostic.aiida.UnitOfMeasurement.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchemaTest {
    private static final LogCaptor LOG_CAPTOR = LogCaptor.forClass(BaseCimFormatterStrategy.class);
    private static final Instant TIMESTAMP = Instant.ofEpochMilli(1729334059);
    private static final UUID PERMISSION_ID = UUID.fromString("3211ea05-d4ab-48ff-8613-8f4791a56606");
    private static final UUID DATA_SOURCE_ID = UUID.fromString("4211ea05-d4ab-48ff-8613-8f4791a56606");
    private static final UUID USER_ID = UUID.fromString("5211ea05-d4ab-48ff-8613-8f4791a56606");
    private static final AiidaRecord AIIDA_RECORD_AT = new AiidaRecord(
            TIMESTAMP,
            AiidaAsset.SUBMETER,
            USER_ID,
            DATA_SOURCE_ID,
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
    private static final AiidaRecord AIIDA_RECORD_FR = new AiidaRecord(
            TIMESTAMP,
            AiidaAsset.SUBMETER,
            USER_ID,
            DATA_SOURCE_ID,
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
    private static final AiidaRecord AIIDA_RECORD_WITH_FAULTY_RECORD = new AiidaRecord(
            TIMESTAMP,
            AiidaAsset.SUBMETER,
            USER_ID,
            DATA_SOURCE_ID,
            List.of(new AiidaRecordValue("BASE",
                                         POSITIVE_ACTIVE_ENERGY,
                                         "ten",
                                         KILO_WATT_HOUR,
                                         "ten",
                                         KILO_WATT_HOUR)
            )
    );
    private static final AiidaRecord AIIDA_RECORD_WITH_UNSUPPORTED_QUANTITY_TYPE = new AiidaRecord(
            TIMESTAMP,
            AiidaAsset.SUBMETER,
            USER_ID,
            DATA_SOURCE_ID,
            List.of(new AiidaRecordValue("PAPP",
                                         POSITIVE_REACTIVE_INSTANTANEOUS_POWER,
                                         "10",
                                         VOLT_AMPERE,
                                         "10",
                                         VOLT_AMPERE)
            )
    );

    private final ClassLoader classLoader = getClass().getClassLoader();

    @Mock
    private ApplicationInformationService applicationInformationService;

    @Mock
    private SchemaFormatterRegistry schemaFormatterRegistry;

    private JsonMapper mapper;

    @BeforeEach
    void setUp() {
        var builder = JsonMapper.builder();
        new AiidaConfiguration().objectMapperCustomizer().customize(builder);
        mapper = builder.build();

        var applicationInformation = mock(ApplicationInformation.class);
        when(applicationInformationService.applicationInformation()).thenReturn(applicationInformation);
        when(applicationInformation.aiidaId()).thenReturn(UUID.randomUUID());
    }

    @Test
    void schemaRaw() throws SchemaFormatterRegistryException, SchemaFormatterException, IOException {
        var permissionMock = mock(Permission.class);
        when(permissionMock.id()).thenReturn(PERMISSION_ID);

        var rawFormatter = new RawFormatter(applicationInformationService, mapper);
        when(schemaFormatterRegistry.formatterFor(AiidaSchema.SMART_METER_P1_RAW)).thenReturn(rawFormatter);
        var formatter = schemaFormatterRegistry.formatterFor(AiidaSchema.SMART_METER_P1_RAW);

        try (var rawRecordStreamAT = classLoader.getResourceAsStream("aiida/record/at/raw_record.json")) {
            var rawRecordBytes = Objects.requireNonNull(rawRecordStreamAT).readAllBytes();
            var expectedRecordJson = new String(rawRecordBytes, StandardCharsets.UTF_8);

            var formattedRawRecordBytes = formatter.format(AIIDA_RECORD_AT, permissionMock);
            var actualRecordJson = new String(formattedRawRecordBytes, StandardCharsets.UTF_8);

            assertEquals(mapper.readTree(expectedRecordJson), mapper.readTree(actualRecordJson));
        }

        try (var rawAiidaRecordFRStream = classLoader.getResourceAsStream("aiida/record/fr/raw_record.json")) {
            var rawRecordBytes = Objects.requireNonNull(rawAiidaRecordFRStream).readAllBytes();
            var expectedRecordJson = new String(rawRecordBytes, StandardCharsets.UTF_8);

            var formattedRawRecordBytes = formatter.format(AIIDA_RECORD_FR, permissionMock);
            var actualRecordJson = new String(formattedRawRecordBytes, StandardCharsets.UTF_8);

            assertEquals(mapper.readTree(expectedRecordJson), mapper.readTree(actualRecordJson));
        }
    }

    @Test
    void schemaCim_v1_04() throws SchemaFormatterException, SchemaFormatterRegistryException {
        LOG_CAPTOR.setLogLevelToTrace();

        var dataNeedId = UUID.fromString("1211ea05-d4ab-48ff-8613-8f4791a56606");
        var permissionId = UUID.fromString("2211ea05-d4ab-48ff-8613-8f4791a56606");
        var cimFormatter = new CimFormatter(applicationInformationService, mapper);

        var permissionMock = mock(Permission.class);
        var dataNeedMock = mock(AiidaLocalDataNeed.class);
        var dataSource = mock(DataSource.class);

        when(permissionMock.id()).thenReturn(permissionId);
        when(permissionMock.dataSource()).thenReturn(dataSource);
        when(permissionMock.dataNeed()).thenReturn(dataNeedMock);
        when(permissionMock.connectionId()).thenReturn("connectionId");

        when(dataNeedMock.dataNeedId()).thenReturn(dataNeedId);

        when(dataSource.countryCode()).thenReturn("AT");

        when(schemaFormatterRegistry.formatterFor(AiidaSchema.SMART_METER_P1_CIM_V1_04)).thenReturn(cimFormatter);

        var formatter = schemaFormatterRegistry.formatterFor(AiidaSchema.SMART_METER_P1_CIM_V1_04);
        assertDoesNotThrow(() -> formatter.format(AIIDA_RECORD_AT, permissionMock));
        assertThrows(CimSchemaFormatterException.class,
                     () -> formatter.format(AIIDA_RECORD_WITH_FAULTY_RECORD, permissionMock));

        formatter.format(AIIDA_RECORD_WITH_UNSUPPORTED_QUANTITY_TYPE, permissionMock);
        assertThat(LOG_CAPTOR.getTraceLogs()).contains("AIIDA Record Value with data tag %s not supported.".formatted(
                POSITIVE_REACTIVE_INSTANTANEOUS_POWER));
    }
}
