// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.schemas.cim.v1_04;

import energy.eddie.aiida.errors.formatter.CimSchemaFormatterException;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.aiida.schemas.cim.BaseCimFormatterStrategy;
import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.cim.v1_04.StandardCodingSchemeTypeList;
import energy.eddie.cim.v1_04.StandardQualityTypeList;
import energy.eddie.cim.v1_04.rtd.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.*;

public class CimStrategy extends BaseCimFormatterStrategy<RTDEnvelope, TimeSeries, StandardCodingSchemeTypeList, QuantityTypeKind> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CimStrategy.class.getName());

    private static final Map<ObisCode, QuantityTypeKind> OBIS_TO_QUANTITY_TYPE = new EnumMap<>(
            Map.ofEntries(
                    Map.entry(ObisCode.POSITIVE_ACTIVE_ENERGY, QuantityTypeKind.TOTALACTIVEENERGYCONSUMED_IMPORT_KWH),
                    Map.entry(ObisCode.NEGATIVE_ACTIVE_ENERGY, QuantityTypeKind.TOTALACTIVEENERGYPRODUCED_EXPORT_KWH),
                    Map.entry(ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER,
                              QuantityTypeKind.INSTANTANEOUSACTIVEPOWERCONSUMPTION_IMPORT__KW),
                    Map.entry(ObisCode.NEGATIVE_ACTIVE_INSTANTANEOUS_POWER,
                              QuantityTypeKind.INSTANTANEOUSACTIVEPOWERGENERATION_EXPORT_KW),
                    Map.entry(ObisCode.INSTANTANEOUS_POWER_FACTOR, QuantityTypeKind.POWERFACTOR),
                    Map.entry(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L1,
                              QuantityTypeKind.INSTANTANEOUSCURRENT_A_ON_PHASEL1),
                    Map.entry(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L2,
                              QuantityTypeKind.INSTANTANEOUSCURRENT_A_ON_PHASEL2),
                    Map.entry(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L3,
                              QuantityTypeKind.INSTANTANEOUSCURRENT_A_ON_PHASEL3),
                    Map.entry(ObisCode.INSTANTANEOUS_VOLTAGE_IN_PHASE_L1,
                              QuantityTypeKind.INSTANTANEOUSVOLTAGE_V_ON_PHASEL1),
                    Map.entry(ObisCode.INSTANTANEOUS_VOLTAGE_IN_PHASE_L2,
                              QuantityTypeKind.INSTANTANEOUSVOLTAGE_V_ON_PHASEL2),
                    Map.entry(ObisCode.INSTANTANEOUS_VOLTAGE_IN_PHASE_L3,
                              QuantityTypeKind.INSTANTANEOUSVOLTAGE_V_ON_PHASEL3)
            )
    );

    @Override
    public RTDEnvelope toRealTimeDataEnvelope(
            UUID aiidaId,
            AiidaRecord aiidaRecord,
            Permission permission
    ) throws CimSchemaFormatterException {
        var dataNeed = dataNeedOfPermissionOrThrow(permission);
        var countryCode = dataSourceOfPermissionOrThrow(permission).countryCode();
        var dataSource = aiidaRecord.dataSource();

        return new RTDEnvelope()
                .withMarketDocument(toRealTimeDataMarketDocument(aiidaRecord, countryCode))
                .withMessageDocumentHeaderCreationDateTime(ZonedDateTime.now(UTC))
                .withMessageDocumentHeaderMetaInformationAsset(dataSource.asset().toString())
                .withMessageDocumentHeaderMetaInformationConnectionId(permission.connectionId())
                .withMessageDocumentHeaderMetaInformationDataNeedId(dataNeed.dataNeedId().toString())
                .withMessageDocumentHeaderMetaInformationDataSourceId(dataSource.id().toString())
                .withMessageDocumentHeaderMetaInformationDocumentType(DOCUMENT_TYPE)
                .withMessageDocumentHeaderMetaInformationFinalCustomerId(aiidaId.toString())
                .withMessageDocumentHeaderMetaInformationPermissionId(permission.id().toString())
                .withMessageDocumentHeaderMetaInformationRegionConnector(REGION_CONNECTOR)
                .withMessageDocumentHeaderMetaInformationRegionCountry(countryCode);
    }

    @Override
    public List<AiidaRecordValue> timeSeriesToAiidaRecordValues(TimeSeries timeSeries) throws CimSchemaFormatterException {
        if (timeSeries == null || timeSeries.getQuantities() == null) {
            throw new CimSchemaFormatterException(new IllegalArgumentException(
                    "TimeSeries or its quantities cannot be null"));
        }

        return timeSeries
                .getQuantities()
                .stream()
                .filter(quantity -> quantity != null && quantity.getType() != null)
                .map(this::quantityToAiidaRecordValue)
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    protected Map<ObisCode, QuantityTypeKind> obisToQuantityTypeKindMap() {
        return OBIS_TO_QUANTITY_TYPE;
    }

    private RTDMarketDocument toRealTimeDataMarketDocument(AiidaRecord aiidaRecord, String countryCode) {
        return new RTDMarketDocument()
                .withCreatedDateTime(ZonedDateTime.now(UTC))
                .withMRID(UUID.randomUUID().toString())
                .withTimeSeries(toTimeSeries(aiidaRecord, countryCode));
    }

    private TimeSeries toTimeSeries(AiidaRecord aiidaRecord, String countryCode) {
        var codingScheme = standardCodingSchemeFromValue(
                countryCode,
                StandardCodingSchemeTypeList::fromValue);
        var codingSchemeValue = codingScheme != null ? codingScheme.value() : null;

        return new TimeSeries()
                .withDateAndOrTimeDateTime(aiidaRecord.timestamp().atZone(UTC))
                .withQuantities(aiidaRecord
                                        .aiidaRecordValues()
                                        .stream()
                                        .filter(this::isAiidaRecordValueSupported)
                                        .map(this::toQuantity)
                                        .toList())
                .withRegisteredResourceMRID(new ResourceIDString()
                                                    .withCodingScheme(codingSchemeValue)
                                                    .withValue(aiidaRecord.dataSource().id().toString()))
                .withVersion(VERSION);
    }

    private Quantity toQuantity(AiidaRecordValue aiidaRecordValue) {
        try {
            return new Quantity()
                    .withQuality(StandardQualityTypeList.AS_PROVIDED.toString())
                    .withQuantity(toBigDecimalOrThrow(aiidaRecordValue))
                    .withType(aiidaRecordValueToQuantityTypeKind(aiidaRecordValue));
        } catch (CimSchemaFormatterException e) {
            LOGGER.error("Error converting AiidaRecordValue to Quantity.", e);
            return new Quantity()
                    .withQuality(StandardQualityTypeList.NOT_AVAILABLE.toString());
        }
    }

    private Optional<AiidaRecordValue> quantityToAiidaRecordValue(Quantity quantity) {
        var obisCode = OBIS_TO_QUANTITY_TYPE.entrySet()
                                            .stream()
                                            .filter(entry -> entry.getValue()
                                                                  .equals(quantity.getType()))
                                            .map(Map.Entry::getKey)
                                            .findFirst()
                                            .orElse(null);
        if (obisCode == null) {
            LOGGER.warn("No matching ObisCode found for QuantityTypeKind: {}", quantity.getType());
            return Optional.empty();
        }

        var quantityValue = quantity.getQuantity().toString();
        var unitOfMeasurement = obisCode.unitOfMeasurement();

        var aiidaRecordValue = new AiidaRecordValue(quantity.getType().name(),
                                                    obisCode,
                                                    quantityValue,
                                                    unitOfMeasurement,
                                                    quantityValue,
                                                    unitOfMeasurement);

        return Optional.of(aiidaRecordValue);
    }
}
