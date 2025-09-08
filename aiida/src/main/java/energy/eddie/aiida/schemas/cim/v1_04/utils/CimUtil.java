package energy.eddie.aiida.schemas.cim.v1_04.utils;

import energy.eddie.aiida.errors.formatter.CimFormatterException;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.dataneed.AiidaLocalDataNeed;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.aiida.models.record.UnitOfMeasurement;
import energy.eddie.aiida.utils.ObisCode;
import energy.eddie.cim.v1_04.rtd.QuantityTypeKind;
import energy.eddie.cim.v1_04.rtd.TimeSeries;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class CimUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(CimUtil.class.getName());
    private static final Map<ObisCode, QuantityTypeKind> OBIS_TO_QUANTITY_TYPE = Map.ofEntries(
            Map.entry(ObisCode.POSITIVE_ACTIVE_ENERGY, QuantityTypeKind.TOTALACTIVEENERGYCONSUMED_IMPORT_KWH),
            Map.entry(ObisCode.NEGATIVE_ACTIVE_ENERGY, QuantityTypeKind.TOTALACTIVEENERGYPRODUCED_EXPORT_KWH),
            Map.entry(ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER,
                      QuantityTypeKind.INSTANTANEOUSACTIVEPOWERCONSUMPTION_IMPORT__KW),
            Map.entry(ObisCode.NEGATIVE_ACTIVE_INSTANTANEOUS_POWER,
                      QuantityTypeKind.INSTANTANEOUSACTIVEPOWERGENERATION_EXPORT_KW),
            Map.entry(ObisCode.INSTANTANEOUS_POWER_FACTOR, QuantityTypeKind.POWERFACTOR),
            Map.entry(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L1, QuantityTypeKind.INSTANTANEOUSCURRENT_A_ON_PHASEL1),
            Map.entry(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L2, QuantityTypeKind.INSTANTANEOUSCURRENT_A_ON_PHASEL2),
            Map.entry(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L3, QuantityTypeKind.INSTANTANEOUSCURRENT_A_ON_PHASEL3),
            Map.entry(ObisCode.INSTANTANEOUS_VOLTAGE_IN_PHASE_L1, QuantityTypeKind.INSTANTANEOUSVOLTAGE_V_ON_PHASEL1),
            Map.entry(ObisCode.INSTANTANEOUS_VOLTAGE_IN_PHASE_L2, QuantityTypeKind.INSTANTANEOUSVOLTAGE_V_ON_PHASEL2),
            Map.entry(ObisCode.INSTANTANEOUS_VOLTAGE_IN_PHASE_L3, QuantityTypeKind.INSTANTANEOUSVOLTAGE_V_ON_PHASEL3)
    );

    private static final Map<QuantityTypeKind, UnitOfMeasurement> QUANTITY_TYPE_TO_UOM = Map.ofEntries(
            Map.entry(QuantityTypeKind.TOTALACTIVEENERGYCONSUMED_IMPORT_KWH, UnitOfMeasurement.KILO_WATT_HOUR),
            Map.entry(QuantityTypeKind.TOTALACTIVEENERGYPRODUCED_EXPORT_KWH, UnitOfMeasurement.KILO_WATT_HOUR),
            Map.entry(QuantityTypeKind.INSTANTANEOUSACTIVEPOWERCONSUMPTION_IMPORT__KW, UnitOfMeasurement.KILO_WATT),
            Map.entry(QuantityTypeKind.INSTANTANEOUSACTIVEPOWERGENERATION_EXPORT_KW, UnitOfMeasurement.KILO_WATT),
            Map.entry(QuantityTypeKind.POWERFACTOR, UnitOfMeasurement.NONE),
            Map.entry(QuantityTypeKind.INSTANTANEOUSCURRENT_A_ON_PHASEL1, UnitOfMeasurement.AMPERE),
            Map.entry(QuantityTypeKind.INSTANTANEOUSCURRENT_A_ON_PHASEL2, UnitOfMeasurement.AMPERE),
            Map.entry(QuantityTypeKind.INSTANTANEOUSCURRENT_A_ON_PHASEL3, UnitOfMeasurement.AMPERE),
            Map.entry(QuantityTypeKind.INSTANTANEOUSVOLTAGE_V_ON_PHASEL1, UnitOfMeasurement.VOLT),
            Map.entry(QuantityTypeKind.INSTANTANEOUSVOLTAGE_V_ON_PHASEL2, UnitOfMeasurement.VOLT),
            Map.entry(QuantityTypeKind.INSTANTANEOUSVOLTAGE_V_ON_PHASEL3, UnitOfMeasurement.VOLT)
    );

    private CimUtil() {
        // utility class
    }

    public static boolean isAiidaRecordValueSupported(AiidaRecordValue recordValue) {
        if (OBIS_TO_QUANTITY_TYPE.containsKey(getDataTag(recordValue))) {
            return true;
        }

        LOGGER.trace("AIIDA Record Value with data tag {} not supported.", recordValue.dataTag());
        return false;
    }

    @Nullable
    public static QuantityTypeKind aiidaRecordValueToQuantityTypeKind(AiidaRecordValue recordValue) {
        return OBIS_TO_QUANTITY_TYPE.get(getDataTag(recordValue));
    }

    public static DataSource dataSourceOfPermissionOrThrow(Permission permission) throws CimFormatterException {
        var dataSource = permission.dataSource();
        if (dataSource == null) {
            throw new CimFormatterException(new IllegalArgumentException("Data source is required for CIM formatting"));
        }

        return dataSource;
    }

    public static AiidaLocalDataNeed dataNeedOfPermissionOrThrow(Permission permission) throws CimFormatterException {
        var dataNeed = permission.dataNeed();
        if (dataNeed == null) {
            throw new CimFormatterException(new IllegalArgumentException("Data need is required for CIM formatting"));
        }

        return dataNeed;
    }

    public static List<AiidaRecordValue> timeSeriesToAiidaRecordValues(TimeSeries timeSeries) throws CimFormatterException {
        if (timeSeries == null || timeSeries.getQuantities() == null) {
            throw new CimFormatterException(new IllegalArgumentException("TimeSeries or its quantities cannot be null"));
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
                             var unitOfMeasurement = Optional.ofNullable(QUANTITY_TYPE_TO_UOM.get(quantity.getType()))
                                                             .orElse(UnitOfMeasurement.UNKNOWN);

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

    private static ObisCode getDataTag(AiidaRecordValue recordValue) {
        Objects.requireNonNull(recordValue, "AiidaRecordValue cannot be null");
        var dataTag = recordValue.dataTag();
        if (dataTag == null) {
            throw new CimFormatterException(new IllegalArgumentException(
                    "Data tag is required for CIM formatting in record: " + recordValue));
        }
        return dataTag;
    }
}