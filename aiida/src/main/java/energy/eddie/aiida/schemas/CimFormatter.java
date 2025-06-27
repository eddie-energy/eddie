package energy.eddie.aiida.schemas;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.aiida.models.record.UnitOfMeasurement;
import energy.eddie.aiida.utils.CimUtils;
import energy.eddie.aiida.utils.ObisCode;
import energy.eddie.cim.v1_04.*;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

public class CimFormatter extends SchemaFormatter {
    private static final String DOCUMENT_TYPE = "near-real-time-market-document";
    private static final String REGION_CONNECTOR = "aiida";
    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final String VERSION = "1.0";

    private final UUID aiidaId;

    public CimFormatter(UUID aiidaId) {
        this.aiidaId = aiidaId;
    }

    @Override
    public byte[] toSchema(AiidaRecord aiidaRecord, ObjectMapper mapper, Permission permission) {
        try {
            return mapper.writeValueAsBytes(toEnvelope(aiidaRecord, permission));
        } catch (JsonProcessingException e) {
            throw new CimFormatterException(e);
        }
    }

    private RTDEnvelope toEnvelope(AiidaRecord aiidaRecord, Permission permission) {
        var dataNeed = Objects.requireNonNull(permission.dataNeed());
        var countryCode = Objects.requireNonNull(permission.dataSource()).countryCode();
        var codingScheme = CimUtils.codingSchemeFromCountryCode(countryCode);
        var codingSchemeValue = codingScheme != null ? codingScheme.value() : null;

        return new RTDEnvelope().withMarketDocument(toMarketDocument(aiidaRecord, codingSchemeValue))
                                .withMessageDocumentHeaderCreationDateTime(ZonedDateTime.of(LocalDateTime.now(), UTC))
                                .withMessageDocumentHeaderMetaInformationAsset(aiidaRecord.asset().toString())
                                .withMessageDocumentHeaderMetaInformationConnectionId(permission.connectionId())
                                .withMessageDocumentHeaderMetaInformationDataNeedId(dataNeed.dataNeedId().toString())
                                .withMessageDocumentHeaderMetaInformationDataSourceId(aiidaRecord.dataSourceId()
                                                                                                 .toString())
                                .withMessageDocumentHeaderMetaInformationDocumentType(DOCUMENT_TYPE)
                                .withMessageDocumentHeaderMetaInformationFinalCustomerId(aiidaId.toString())
                                .withMessageDocumentHeaderMetaInformationPermissionId(permission.permissionId()
                                                                                                .toString())
                                .withMessageDocumentHeaderMetaInformationRegionConnector(REGION_CONNECTOR)
                                .withMessageDocumentHeaderMetaInformationRegionCountry(countryCode);
    }

    private RTDMarketDocument toMarketDocument(AiidaRecord aiidaRecord, @Nullable String codingScheme) {
        return new RTDMarketDocument()
                .withCreatedDateTime(ZonedDateTime.of(LocalDateTime.now(), UTC))
                .withMRID(UUID.randomUUID().toString())
                .withTimeSeries(toTimeSeries(aiidaRecord, codingScheme));
    }

    private TimeSeries toTimeSeries(AiidaRecord aiidaRecord, @Nullable String codingScheme) {
        return new TimeSeries().withDateAndOrTimeDateTime(aiidaRecord.timestamp().atZone(UTC))
                               .withQuantities(aiidaRecord
                                                       .aiidaRecordValues()
                                                       .stream()
                                                       .filter(this::isDecimalValue)
                                                       .map(aiidaRecordValue -> new Quantity()
                                                               .withQuality(StandardQualityTypeList.AS_PROVIDED.toString())
                                                               .withQuantity(toBigDecimal(aiidaRecordValue))
                                                               .withType(toQuantityTypeKind(aiidaRecordValue)))
                                                       .toList())
                               .withRegisteredResourceMRID(new ResourceIDString()
                                                                   .withCodingScheme(codingScheme)
                                                                   .withValue(aiidaRecord.dataSourceId().toString()))
                               .withVersion(VERSION);
    }

    @Nullable
    private QuantityTypeKind toQuantityTypeKind(AiidaRecordValue aiidaRecordValue) {
        return switch (Objects.requireNonNull(aiidaRecordValue.dataTag())) {
            case POSITIVE_ACTIVE_ENERGY -> QuantityTypeKind.TOTALACTIVEENERGYCONSUMED_IMPORT_KWH;
            case NEGATIVE_ACTIVE_ENERGY -> QuantityTypeKind.TOTALACTIVEENERGYPRODUCED_EXPORT_KWH;
            case POSITIVE_ACTIVE_INSTANTANEOUS_POWER -> QuantityTypeKind.INSTANTANEOUSACTIVEPOWERCONSUMPTION_IMPORT__KW;
            case NEGATIVE_ACTIVE_INSTANTANEOUS_POWER -> QuantityTypeKind.INSTANTANEOUSACTIVEPOWERGENERATION_EXPORT_KW;
            case INSTANTANEOUS_POWER_FACTOR -> QuantityTypeKind.POWERFACTOR;
            case INSTANTANEOUS_CURRENT_IN_PHASE_L1 -> QuantityTypeKind.INSTANTANEOUSCURRENT_A_ON_PHASEL1;
            case INSTANTANEOUS_CURRENT_IN_PHASE_L2 -> QuantityTypeKind.INSTANTANEOUSCURRENT_A_ON_PHASEL2;
            case INSTANTANEOUS_CURRENT_IN_PHASE_L3 -> QuantityTypeKind.INSTANTANEOUSCURRENT_A_ON_PHASEL3;
            case INSTANTANEOUS_VOLTAGE_IN_PHASE_L1 -> QuantityTypeKind.INSTANTANEOUSVOLTAGE_V_ON_PHASEL1;
            case INSTANTANEOUS_VOLTAGE_IN_PHASE_L2 -> QuantityTypeKind.INSTANTANEOUSVOLTAGE_V_ON_PHASEL2;
            case INSTANTANEOUS_VOLTAGE_IN_PHASE_L3 -> QuantityTypeKind.INSTANTANEOUSVOLTAGE_V_ON_PHASEL3;
            default -> null;
        };
    }

    private boolean isDecimalValue(AiidaRecordValue aiidaRecordValue) {
        if (Objects.requireNonNull(aiidaRecordValue.dataTag()) == ObisCode.INSTANTANEOUS_POWER_FACTOR) {
            return true;
        }

        return aiidaRecordValue.unitOfMeasurement() != UnitOfMeasurement.NONE &&
               aiidaRecordValue.unitOfMeasurement() != UnitOfMeasurement.UNKNOWN;
    }

    private BigDecimal toBigDecimal(AiidaRecordValue aiidaRecordValue) {
        try {
            return new BigDecimal(aiidaRecordValue.value());
        } catch (NumberFormatException e) {
            throw new CimFormatterException(e);
        }
    }

    static class CimFormatterException extends RuntimeException {
        public CimFormatterException(Exception e) {
            super(e);
        }
    }
}
