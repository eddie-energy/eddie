// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.schemas.cim.v1_04;

import energy.eddie.aiida.errors.formatter.CimSchemaFormatterException;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.dataneed.AiidaLocalDataNeed;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.api.agnostic.aiida.AiidaAsset;
import energy.eddie.cim.v1_04.rtd.Quantity;
import energy.eddie.cim.v1_04.rtd.QuantityTypeKind;
import energy.eddie.cim.v1_04.rtd.TimeSeries;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static energy.eddie.api.agnostic.aiida.ObisCode.*;
import static energy.eddie.api.agnostic.aiida.UnitOfMeasurement.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CimStrategyTest {
    private static final Instant TIMESTAMP = Instant.parse("2025-01-01T12:34:56Z");
    private static final UUID AIIDA_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID DATA_SOURCE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID DATA_NEED_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID PERMISSION_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private final CimStrategy strategy = new CimStrategy();

    @Test
    void toRealTimeDataEnvelope_mapsMetadataAndSupportedQuantities() throws CimSchemaFormatterException {
        // Given
        var permission = permission("AT");
        var aiidaRecord = new AiidaRecord(
                TIMESTAMP,
                AiidaAsset.SUBMETER,
                AIIDA_ID,
                DATA_SOURCE_ID,
                List.of(
                        new AiidaRecordValue("1-0:1.8.0", POSITIVE_ACTIVE_ENERGY, "100", KILO_WATT_HOUR, "100", KILO_WATT_HOUR),
                        new AiidaRecordValue("1-0:96.1.0", DEVICE_ID_1, "device", NONE, "device", NONE),
                        new AiidaRecordValue("1-0:32.7.0", INSTANTANEOUS_VOLTAGE_IN_PHASE_L1, "230", VOLT, "230", VOLT)
                )
        );

        // When
        var envelope = strategy.toRealTimeDataEnvelope(AIIDA_ID, aiidaRecord, permission);

        // Then
        assertThat(envelope.getMessageDocumentHeaderMetaInformationConnectionId()).isEqualTo("connection-123");
        assertThat(envelope.getMessageDocumentHeaderMetaInformationDataNeedId()).isEqualTo(DATA_NEED_ID.toString());
        assertThat(envelope.getMessageDocumentHeaderMetaInformationPermissionId()).isEqualTo(PERMISSION_ID.toString());
        assertThat(envelope.getMessageDocumentHeaderMetaInformationDataSourceId()).isEqualTo(DATA_SOURCE_ID.toString());
        assertThat(envelope.getMessageDocumentHeaderMetaInformationFinalCustomerId()).isEqualTo(AIIDA_ID.toString());
        assertThat(envelope.getMessageDocumentHeaderMetaInformationRegionConnector()).isEqualTo("aiida");
        assertThat(envelope.getMessageDocumentHeaderMetaInformationRegionCountry()).isEqualTo("AT");
        assertThat(envelope.getMessageDocumentHeaderMetaInformationDocumentType()).isEqualTo("near-real-time-market-document");
        assertThat(envelope.getMessageDocumentHeaderMetaInformationAsset()).isEqualTo(AiidaAsset.SUBMETER.toString());
        assertThat(envelope.getMessageDocumentHeaderCreationDateTime()).isNotNull();
        assertThat(envelope.getMessageDocumentHeaderCreationDateTime().getZone()).isEqualTo(ZoneId.of("UTC"));

        assertThat(envelope.getMarketDocument()).isNotNull();
        assertThat(envelope.getMarketDocument().getTimeSeries()).hasSize(1);

        var timeSeries = envelope.getMarketDocument().getTimeSeries().getFirst();
        assertThat(timeSeries.getVersion()).isEqualTo("1.0");
        assertThat(timeSeries.getDateAndOrTimeDateTime()).isEqualTo(TIMESTAMP.atZone(ZoneId.of("UTC")));
        assertThat(timeSeries.getRegisteredResourceMRID().getValue()).isEqualTo(DATA_SOURCE_ID.toString());
        assertThat(timeSeries.getRegisteredResourceMRID().getCodingScheme()).isEqualTo("NAT");

        assertThat(timeSeries.getQuantities()).hasSize(2);
        assertThat(timeSeries.getQuantities().stream().map(Quantity::getType)).containsExactly(
                QuantityTypeKind.TOTALACTIVEENERGYCONSUMED_IMPORT_KWH,
                QuantityTypeKind.INSTANTANEOUSVOLTAGE_V_ON_PHASEL1
        );
    }

    @Test
    void toRealTimeDataEnvelope_setsNullCodingSchemeForUnknownCountryCode() throws CimSchemaFormatterException {
        // Given
        var permission = permission("");
        var aiidaRecord = new AiidaRecord(
                TIMESTAMP,
                AiidaAsset.SUBMETER,
                AIIDA_ID,
                DATA_SOURCE_ID,
                List.of(new AiidaRecordValue("1-0:1.8.0", POSITIVE_ACTIVE_ENERGY, "10", KILO_WATT_HOUR, "10", KILO_WATT_HOUR))
        );

        // When
        var envelope = strategy.toRealTimeDataEnvelope(AIIDA_ID, aiidaRecord, permission);

        // Then
        var timeSeries = envelope.getMarketDocument().getTimeSeries().getFirst();
        assertThat(timeSeries.getRegisteredResourceMRID().getCodingScheme()).isNull();
    }

    @Test
    void timeSeriesToAiidaRecordValues_convertsKnownQuantityAndSkipsNullEntries() throws CimSchemaFormatterException {
        // Given
        var timeSeries = new TimeSeries().withQuantities(
                null,
                new Quantity().withQuantity(new BigDecimal("42")),
                new Quantity()
                        .withType(QuantityTypeKind.TOTALACTIVEENERGYCONSUMED_IMPORT_KWH)
                        .withQuantity(new BigDecimal("12.34"))
        );

        // When
        var result = strategy.timeSeriesToAiidaRecordValues(timeSeries);

        // Then
        assertThat(result).hasSize(1);
        var value = result.getFirst();
        assertThat(value.rawTag()).isEqualTo("TOTALACTIVEENERGYCONSUMED_IMPORT_KWH");
        assertThat(value.dataTag()).isEqualTo(POSITIVE_ACTIVE_ENERGY);
        assertThat(value.value()).isEqualTo("12.34");
        assertThat(value.unitOfMeasurement()).isEqualTo(KILO_WATT_HOUR);
    }

    @Test
    void timeSeriesToAiidaRecordValues_throwsForNullOrMissingQuantities() {
        assertThatThrownBy(() -> strategy.timeSeriesToAiidaRecordValues(null))
                .isInstanceOf(CimSchemaFormatterException.class)
                .hasRootCauseMessage("TimeSeries or its quantities cannot be null");

        var timeSeries = mock(TimeSeries.class);
        when(timeSeries.getQuantities()).thenReturn(null);

        assertThatThrownBy(() -> strategy.timeSeriesToAiidaRecordValues(timeSeries))
                .isInstanceOf(CimSchemaFormatterException.class)
                .hasRootCauseMessage("TimeSeries or its quantities cannot be null");
    }

    private static Permission permission(String countryCode) {
        var permission = mock(Permission.class);
        var dataSource = mock(DataSource.class);
        var dataNeed = mock(AiidaLocalDataNeed.class);

        when(permission.id()).thenReturn(PERMISSION_ID);
        when(permission.connectionId()).thenReturn("connection-123");
        when(permission.dataSource()).thenReturn(dataSource);
        when(permission.dataNeed()).thenReturn(dataNeed);
        when(dataSource.countryCode()).thenReturn(countryCode);
        when(dataNeed.dataNeedId()).thenReturn(DATA_NEED_ID);

        return permission;
    }
}
