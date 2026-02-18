// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.schemas.cim.v1_12;

import energy.eddie.aiida.errors.formatter.CimSchemaFormatterException;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.aiida.schemas.cim.BaseCimFormatterStrategy;
import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.cim.v1_12.StandardCodingSchemeTypeList;
import energy.eddie.cim.v1_12.StandardQualityTypeList;
import energy.eddie.cim.v1_12.rtd.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.*;

public class CimStrategy extends BaseCimFormatterStrategy<RTDEnvelope, TimeSeries, StandardCodingSchemeTypeList, QuantityTypeKind> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CimStrategy.class.getName());

    private static final Map<ObisCode, QuantityTypeKind> OBIS_TO_QUANTITY_TYPE =
            new EnumMap<>(Map.ofEntries(
                    // --- Active Energy (Consumption) ---
                    Map.entry(ObisCode.POSITIVE_ACTIVE_ENERGY,
                              QuantityTypeKind.TOTAL_ACTIVE_ENERGY_CONSUMED_KWH),
                    Map.entry(ObisCode.POSITIVE_ACTIVE_ENERGY_IN_PHASE_L1,
                              QuantityTypeKind.TOTAL_ACTIVE_ENERGY_CONSUMED_KWH_IN_PHASE_L1),
                    Map.entry(ObisCode.POSITIVE_ACTIVE_ENERGY_IN_PHASE_L2,
                              QuantityTypeKind.TOTAL_ACTIVE_ENERGY_CONSUMED_KWH_IN_PHASE_L2),
                    Map.entry(ObisCode.POSITIVE_ACTIVE_ENERGY_IN_PHASE_L3,
                              QuantityTypeKind.TOTAL_ACTIVE_ENERGY_CONSUMED_KWH_IN_PHASE_L3),

                    // --- Active Energy (Production) ---
                    Map.entry(ObisCode.NEGATIVE_ACTIVE_ENERGY,
                              QuantityTypeKind.TOTAL_ACTIVE_ENERGY_PRODUCED_KWH),
                    Map.entry(ObisCode.NEGATIVE_ACTIVE_ENERGY_IN_PHASE_L1,
                              QuantityTypeKind.TOTAL_ACTIVE_ENERGY_PRODUCED_KWH_IN_PHASE_L1),
                    Map.entry(ObisCode.NEGATIVE_ACTIVE_ENERGY_IN_PHASE_L2,
                              QuantityTypeKind.TOTAL_ACTIVE_ENERGY_PRODUCED_KWH_IN_PHASE_L2),
                    Map.entry(ObisCode.NEGATIVE_ACTIVE_ENERGY_IN_PHASE_L3,
                              QuantityTypeKind.TOTAL_ACTIVE_ENERGY_PRODUCED_KWH_IN_PHASE_L3),

                    // --- Active Power ---
                    Map.entry(ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER,
                              QuantityTypeKind.INSTANTANEOUS_ACTIVE_POWER_CONSUMPTION_KW),
                    Map.entry(ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER_IN_PHASE_L1,
                              QuantityTypeKind.INSTANTANEOUS_ACTIVE_POWER_CONSUMPTION_KW_IN_PHASE_L1),
                    Map.entry(ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER_IN_PHASE_L2,
                              QuantityTypeKind.INSTANTANEOUS_ACTIVE_POWER_CONSUMPTION_KW_IN_PHASE_L2),
                    Map.entry(ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER_IN_PHASE_L3,
                              QuantityTypeKind.INSTANTANEOUS_ACTIVE_POWER_CONSUMPTION_KW_IN_PHASE_L3),
                    Map.entry(ObisCode.NEGATIVE_ACTIVE_INSTANTANEOUS_POWER,
                              QuantityTypeKind.INSTANTANEOUS_ACTIVE_POWER_GENERATION_KW),

                    // --- Reactive Power ---
                    Map.entry(ObisCode.POSITIVE_REACTIVE_INSTANTANEOUS_POWER,
                              QuantityTypeKind.INSTANTANEOUS_REACTIVE_POWER_CONSUMPTION_KVAR),
                    Map.entry(ObisCode.POSITIVE_REACTIVE_INSTANTANEOUS_POWER_IN_PHASE_L1,
                              QuantityTypeKind.INSTANTANEOUS_REACTIVE_POWER_CONSUMPTION_KVAR_IN_PHASE_L1),
                    Map.entry(ObisCode.POSITIVE_REACTIVE_INSTANTANEOUS_POWER_IN_PHASE_L2,
                              QuantityTypeKind.INSTANTANEOUS_REACTIVE_POWER_CONSUMPTION_KVAR_IN_PHASE_L2),
                    Map.entry(ObisCode.POSITIVE_REACTIVE_INSTANTANEOUS_POWER_IN_PHASE_L3,
                              QuantityTypeKind.INSTANTANEOUS_REACTIVE_POWER_CONSUMPTION_KVAR_IN_PHASE_L3),
                    Map.entry(ObisCode.NEGATIVE_REACTIVE_INSTANTANEOUS_POWER,
                              QuantityTypeKind.INSTANTANEOUS_REACTIVE_POWER_GENERATION_KVAR),

                    // --- Current ---
                    Map.entry(ObisCode.MAXIMUM_CURRENT,
                              QuantityTypeKind.MAXIMUM_CURRENT_A),
                    Map.entry(ObisCode.MAXIMUM_CURRENT_IN_PHASE_L1,
                              QuantityTypeKind.MAXIMUM_CURRENT_A_IN_PHASE_L1),
                    Map.entry(ObisCode.MAXIMUM_CURRENT_IN_PHASE_L2,
                              QuantityTypeKind.MAXIMUM_CURRENT_A_IN_PHASE_L2),
                    Map.entry(ObisCode.MAXIMUM_CURRENT_IN_PHASE_L3,
                              QuantityTypeKind.MAXIMUM_CURRENT_A_IN_PHASE_L3),
                    Map.entry(ObisCode.INSTANTANEOUS_CURRENT,
                              QuantityTypeKind.INSTANTANEOUS_CURRENT_A),
                    Map.entry(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L1,
                              QuantityTypeKind.INSTANTANEOUS_CURRENT_A_IN_PHASE_L1),
                    Map.entry(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L2,
                              QuantityTypeKind.INSTANTANEOUS_CURRENT_A_IN_PHASE_L2),
                    Map.entry(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L3,
                              QuantityTypeKind.INSTANTANEOUS_CURRENT_A_IN_PHASE_L3),
                    Map.entry(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_NEUTRAL,
                              QuantityTypeKind.INSTANTANEOUS_CURRENT_A_IN_PHASE_NEUTRAL),

                    // --- Power Factor ---
                    Map.entry(ObisCode.INSTANTANEOUS_POWER_FACTOR,
                              QuantityTypeKind.INSTANTANEOUS_POWERFACTOR),
                    Map.entry(ObisCode.INSTANTANEOUS_POWER_FACTOR_IN_PHASE_L1,
                              QuantityTypeKind.INSTANTANEOUS_POWER_FACTOR_IN_PHASE_L1),
                    Map.entry(ObisCode.INSTANTANEOUS_POWER_FACTOR_IN_PHASE_L2,
                              QuantityTypeKind.INSTANTANEOUS_POWER_FACTOR_IN_PHASE_L2),
                    Map.entry(ObisCode.INSTANTANEOUS_POWER_FACTOR_IN_PHASE_L3,
                              QuantityTypeKind.INSTANTANEOUS_POWER_FACTOR_IN_PHASE_L3),

                    // --- Voltage ---
                    Map.entry(ObisCode.INSTANTANEOUS_VOLTAGE,
                              QuantityTypeKind.INSTANTANEOUS_VOLTAGE_V),
                    Map.entry(ObisCode.INSTANTANEOUS_VOLTAGE_IN_PHASE_L1,
                              QuantityTypeKind.INSTANTANEOUS_VOLTAGE_V_IN_PHASE_L1),
                    Map.entry(ObisCode.INSTANTANEOUS_VOLTAGE_IN_PHASE_L2,
                              QuantityTypeKind.INSTANTANEOUS_VOLTAGE_V_IN_PHASE_L2),
                    Map.entry(ObisCode.INSTANTANEOUS_VOLTAGE_IN_PHASE_L3,
                              QuantityTypeKind.INSTANTANEOUS_VOLTAGE_V_IN_PHASE_L3),

                    // --- Frequency ---
                    Map.entry(ObisCode.FREQUENCY,
                              QuantityTypeKind.FREQUENCY_HZ)
            ));


    @Override
    public RTDEnvelope toRealTimeDataEnvelope(
            UUID aiidaId,
            AiidaRecord aiidaRecord,
            Permission permission
    ) throws CimSchemaFormatterException {
        var countryCode = dataSourceOfPermissionOrThrow(permission).countryCode();

        return new RTDEnvelope()
                .withMessageDocumentHeader(toMessageDocumentHeader(aiidaId, aiidaRecord, permission, countryCode))
                .withMarketDocument(toRealTimeDataMarketDocument(aiidaRecord, countryCode));
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

    private MessageDocumentHeader toMessageDocumentHeader(
            UUID aiidaId,
            AiidaRecord aiidaRecord,
            Permission permission,
            String countryCode
    ) throws CimSchemaFormatterException {
        return new MessageDocumentHeader()
                .withCreationDateTime(ZonedDateTime.now(UTC))
                .withMetaInformation(toMetaInformation(aiidaId, aiidaRecord, permission, countryCode));
    }

    private MetaInformation toMetaInformation(
            UUID aiidaId,
            AiidaRecord aiidaRecord,
            Permission permission,
            String countryCode
    ) throws CimSchemaFormatterException {
        var dataNeed = dataNeedOfPermissionOrThrow(permission);
        var dataSource = aiidaRecord.dataSource();

        return new MetaInformation()
                .withAsset(toAsset(dataSource))
                .withConnectionId(permission.connectionId())
                .withDataNeedId(dataNeed.dataNeedId().toString())
                .withDataSourceId(dataSource.id().toString())
                .withDocumentType(DOCUMENT_TYPE)
                .withFinalCustomerId(aiidaId.toString())
                .withRequestPermissionId(permission.id().toString())
                .withRegionConnector(REGION_CONNECTOR)
                .withRegionCountry(countryCode);
    }

    private Asset toAsset(DataSource dataSource) {
        return new Asset()
                .withType(dataSource.asset().toString())
                .withMeterId(dataSource.meterId())
                .withOperatorId(dataSource.operatorId());
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
                    .withQuality(StandardQualityTypeList.INCOMPLETE.toString());
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

        var aiidaRecord = new AiidaRecordValue(quantity.getType().name(),
                                               obisCode,
                                               quantityValue,
                                               unitOfMeasurement,
                                               quantityValue,
                                               unitOfMeasurement);
        return Optional.of(aiidaRecord);
    }
}