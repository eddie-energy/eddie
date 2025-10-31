package energy.eddie.aiida.schemas.cim.v1_06;

import com.pivovarit.function.ThrowingFunction;
import com.pivovarit.function.ThrowingPredicate;
import energy.eddie.aiida.errors.formatter.CimSchemaFormatterException;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.aiida.schemas.cim.BaseCimFormatterStrategy;
import energy.eddie.aiida.utils.ObisCode;
import energy.eddie.cim.v1_06.StandardCodingSchemeTypeList;
import energy.eddie.cim.v1_06.StandardQualityTypeList;
import energy.eddie.cim.v1_06.rtd.*;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class CimStrategy extends BaseCimFormatterStrategy<RTDEnvelope, RTDMarketDocument, TimeSeries, Quantity, StandardCodingSchemeTypeList, QuantityTypeKind> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CimStrategy.class.getName());

    private static final Map<ObisCode, QuantityTypeKind> OBIS_TO_QUANTITY_TYPE = Map.ofEntries(
            Map.entry(ObisCode.POSITIVE_ACTIVE_ENERGY,
                      QuantityTypeKind.TOTALACTIVEENERGYCONSUMED_IMPORT_KWH),
            Map.entry(ObisCode.POSITIVE_ACTIVE_ENERGY_IN_PHASE_L1,
                      QuantityTypeKind.TOTALACTIVEENERGYCONSUMED_IMPORT_KWH_IN_PHASE_L1),
            Map.entry(ObisCode.POSITIVE_ACTIVE_ENERGY_IN_PHASE_L2,
                      QuantityTypeKind.TOTALACTIVEENERGYCONSUMED_IMPORT_KWH_IN_PHASE_L2),
            Map.entry(ObisCode.POSITIVE_ACTIVE_ENERGY_IN_PHASE_L3,
                      QuantityTypeKind.TOTALACTIVEENERGYCONSUMED_IMPORT_KWH_IN_PHASE_L3),
            Map.entry(ObisCode.NEGATIVE_ACTIVE_ENERGY,
                      QuantityTypeKind.TOTALACTIVEENERGYPRODUCED_EXPORT_KWH),
            Map.entry(ObisCode.NEGATIVE_ACTIVE_ENERGY_IN_PHASE_L1,
                      QuantityTypeKind.TOTALACTIVEENERGYPRODUCED_IMPORT_KWH_IN_PHASE_L1),
            Map.entry(ObisCode.NEGATIVE_ACTIVE_ENERGY_IN_PHASE_L2,
                      QuantityTypeKind.TOTALACTIVEENERGYPRODUCED_IMPORT_KWH_IN_PHASE_L2),
            Map.entry(ObisCode.NEGATIVE_ACTIVE_ENERGY_IN_PHASE_L3,
                      QuantityTypeKind.TOTALACTIVEENERGYPRODUCED_IMPORT_KWH_IN_PHASE_L3),
            Map.entry(ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER,
                      QuantityTypeKind.INSTANTANEOUSACTIVEPOWERCONSUMPTION_IMPORT_KW),
            Map.entry(ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER_IN_PHASE_L1,
                      QuantityTypeKind.INSTANTANEOUSACTIVEPOWERCONSUMPTION_IMPORT_KW_IN_PHASE_L1),
            Map.entry(ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER_IN_PHASE_L2,
                      QuantityTypeKind.INSTANTANEOUSACTIVEPOWERCONSUMPTION_IMPORT_KW_IN_PHASE_L2),
            Map.entry(ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER_IN_PHASE_L3,
                      QuantityTypeKind.INSTANTANEOUSACTIVEPOWERCONSUMPTION_IMPORT_KW_IN_PHASE_L3),
            Map.entry(ObisCode.POSITIVE_REACTIVE_INSTANTANEOUS_POWER,
                      QuantityTypeKind.INSTANTANEOUSREACTIVEPOWERCONSUMPTION_IMPORT_KW),
            Map.entry(ObisCode.POSITIVE_REACTIVE_INSTANTANEOUS_POWER_IN_PHASE_L1,
                      QuantityTypeKind.INSTANTANEOUSREACTIVEPOWERCONSUMPTION_IMPORT_KW_IN_PHASE_L1),
            Map.entry(ObisCode.POSITIVE_REACTIVE_INSTANTANEOUS_POWER_IN_PHASE_L2,
                      QuantityTypeKind.INSTANTANEOUSREACTIVEPOWERCONSUMPTION_IMPORT_KW_IN_PHASE_L2),
            Map.entry(ObisCode.POSITIVE_REACTIVE_INSTANTANEOUS_POWER_IN_PHASE_L3,
                      QuantityTypeKind.INSTANTANEOUSREACTIVEPOWERCONSUMPTION_IMPORT_KW_IN_PHASE_L3),
            Map.entry(ObisCode.NEGATIVE_ACTIVE_INSTANTANEOUS_POWER,
                      QuantityTypeKind.INSTANTANEOUSACTIVEPOWERGENERATION_EXPORT_KW),
            Map.entry(ObisCode.NEGATIVE_REACTIVE_INSTANTANEOUS_POWER,
                      QuantityTypeKind.INSTANTANEOUSREACTIVEPOWERGENERATION_EXPORT_KW),
            Map.entry(ObisCode.INSTANTANEOUS_POWER_FACTOR,
                      QuantityTypeKind.INSTANTANEOUSPOWERFACTOR),
            Map.entry(ObisCode.INSTANTANEOUS_POWER_FACTOR_IN_PHASE_L1,
                      QuantityTypeKind.INSTANTANEOUSPOWERFACTOR_IN_PHASE_L1),
            Map.entry(ObisCode.INSTANTANEOUS_POWER_FACTOR_IN_PHASE_L2,
                      QuantityTypeKind.INSTANTANEOUSPOWERFACTOR_IN_PHASE_L2),
            Map.entry(ObisCode.INSTANTANEOUS_POWER_FACTOR_IN_PHASE_L3,
                      QuantityTypeKind.INSTANTANEOUSPOWERFACTOR_IN_PHASE_L3),
            // TODO: INSTANTANEOUS_CURRENT is not added in CIM yet
            Map.entry(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L1,
                      QuantityTypeKind.INSTANTANEOUSCURRENT_A_IN_PHASE_L1),
            Map.entry(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L2,
                      QuantityTypeKind.INSTANTANEOUSCURRENT_A_IN_PHASE_L2),
            Map.entry(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L3,
                      QuantityTypeKind.INSTANTANEOUSCURRENT_A_IN_PHASE_L3),
            // TODO: INSTANTANEOUSVOLTAGE_A_IN_PHASE_NEUTRAL should be called INSTANTANEOUSCURRENT_A_IN_PHASE_NEUTRAL
            Map.entry(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_NEUTRAL,
                      QuantityTypeKind.INSTANTANEOUSVOLTAGE_A_IN_PHASE_NEUTRAL),
            Map.entry(ObisCode.INSTANTANEOUS_VOLTAGE,
                      QuantityTypeKind.INSTANTANEOUSVOLTAGE_V),
            Map.entry(ObisCode.INSTANTANEOUS_VOLTAGE_IN_PHASE_L1,
                      QuantityTypeKind.INSTANTANEOUSVOLTAGE_V_IN_PHASE_L1),
            Map.entry(ObisCode.INSTANTANEOUS_VOLTAGE_IN_PHASE_L2,
                      QuantityTypeKind.INSTANTANEOUSVOLTAGE_V_IN_PHASE_L2),
            Map.entry(ObisCode.INSTANTANEOUS_VOLTAGE_IN_PHASE_L3,
                      QuantityTypeKind.INSTANTANEOUSVOLTAGE_V_IN_PHASE_L3)
    );

    @Override
    public List<AiidaRecordValue> timeSeriesToAiidaRecordValues(TimeSeries timeSeries) throws CimSchemaFormatterException {
        if (timeSeries == null || timeSeries.getQuantities() == null) {
            throw new CimSchemaFormatterException(new IllegalArgumentException("TimeSeries or its quantities cannot be null"));
        }

        return timeSeries.getQuantities()
                         .stream()
                         .filter(Objects::nonNull)
                         .filter(quantity -> quantity.getType() != null)
                         .map(quantity -> {
                             var obisCode = OBIS_TO_QUANTITY_TYPE.entrySet()
                                                                 .stream()
                                                                 .filter(entry -> entry.getValue()
                                                                                       .equals(quantity.getType()))
                                                                 .map(Map.Entry::getKey)
                                                                 .findFirst()
                                                                 .orElse(null);
                             if (obisCode == null) {
                                 LOGGER.warn("No matching ObisCode found for QuantityTypeKind: {}", quantity.getType());
                                 return null;
                             }

                             var quantityValue = quantity.getQuantity().toString();
                             var unitOfMeasurement = obisCode.unitOfMeasurement();

                             return new AiidaRecordValue(quantity.getType().name(),
                                                         obisCode,
                                                         quantityValue,
                                                         unitOfMeasurement,
                                                         quantityValue,
                                                         unitOfMeasurement);
                         })
                         .filter(Objects::nonNull)
                         .toList();
    }

    @Override
    protected RTDMarketDocument toRealTimeDataMarketDocument(AiidaRecord aiidaRecord, @Nullable String codingScheme) {
        return new RTDMarketDocument()
                .withCreatedDateTime(ZonedDateTime.now(UTC))
                .withMRID(UUID.randomUUID().toString())
                .withTimeSeries(toTimeSeries(aiidaRecord, codingScheme));
    }

    @Override
    protected TimeSeries toTimeSeries(AiidaRecord aiidaRecord, @Nullable String codingScheme) {
        return new TimeSeries().withDateAndOrTimeDateTime(aiidaRecord.timestamp().atZone(UTC))
                               .withQuantities(aiidaRecord
                                                       .aiidaRecordValues()
                                                       .stream()
                                                       .filter(ThrowingPredicate.sneaky(this::isAiidaRecordValueSupported))
                                                       .map(ThrowingFunction.sneaky(this::toQuantity))
                                                       .toList())
                               .withRegisteredResourceMRID(new ResourceIDString()
                                                                   .withCodingScheme(codingScheme)
                                                                   .withValue(aiidaRecord.dataSourceId()
                                                                                         .toString()))
                               .withVersion(VERSION);
    }

    @Override
    protected Quantity toQuantity(AiidaRecordValue aiidaRecordValue) throws CimSchemaFormatterException {
        return new Quantity().withQuality(StandardQualityTypeList.AS_PROVIDED.toString())
                             .withQuantity(toBigDecimalOrThrow(aiidaRecordValue))
                             .withType(aiidaRecordValueToQuantityTypeKind(aiidaRecordValue));
    }

    @Override
    @Nullable
    protected StandardCodingSchemeTypeList standardCodingSchemeFromCountryCode(String countryCode) {
        return standardCodingSchemeFromValue(countryCode, StandardCodingSchemeTypeList::fromValue);
    }

    @Override
    protected Map<ObisCode, QuantityTypeKind> obisToQuantityTypeKindMap() {
        return OBIS_TO_QUANTITY_TYPE;
    }

    @Override
    public RTDEnvelope toRealTimeDataEnvelope(
            UUID aiidaId,
            AiidaRecord aiidaRecord,
            Permission permission
    ) throws CimSchemaFormatterException {
        var dataNeed = dataNeedOfPermissionOrThrow(permission);
        var countryCode = dataSourceOfPermissionOrThrow(permission).countryCode();
        var codingScheme = standardCodingSchemeFromCountryCode(countryCode);
        var codingSchemeValue = codingScheme != null ? codingScheme.value() : null;

        return new RTDEnvelope().withMarketDocument(toRealTimeDataMarketDocument(aiidaRecord, codingSchemeValue))
                                .withMessageDocumentHeaderCreationDateTime(ZonedDateTime.now(UTC))
                                .withMessageDocumentHeaderMetaInformationAsset(aiidaRecord.asset().toString())
                                .withMessageDocumentHeaderMetaInformationConnectionId(permission.connectionId())
                                .withMessageDocumentHeaderMetaInformationDataNeedId(dataNeed.dataNeedId().toString())
                                .withMessageDocumentHeaderMetaInformationDataSourceId(aiidaRecord.dataSourceId()
                                                                                                 .toString())
                                .withMessageDocumentHeaderMetaInformationDocumentType(DOCUMENT_TYPE)
                                .withMessageDocumentHeaderMetaInformationFinalCustomerId(aiidaId.toString())
                                .withMessageDocumentHeaderMetaInformationPermissionId(permission.id().toString())
                                .withMessageDocumentHeaderMetaInformationRegionConnector(REGION_CONNECTOR)
                                .withMessageDocumentHeaderMetaInformationRegionCountry(countryCode);
    }
}