package energy.eddie.aiida.adapters.datasource.fr.transformer.history;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import energy.eddie.aiida.models.record.UnitOfMeasurement;
import energy.eddie.aiida.utils.ObisCode;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum HistoryModeEntry {
    ADCO(ObisCode.METER_SERIAL, UnitOfMeasurement.NONE),
    OPTARIF(ObisCode.UNKNOWN, UnitOfMeasurement.NONE),
    ISOUSC(ObisCode.UNKNOWN, UnitOfMeasurement.AMPERE),
    BASE(ObisCode.POSITIVE_ACTIVE_ENERGY, UnitOfMeasurement.WATT_HOUR),
    HCHC(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR),
    HCHP(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR),
    EJPHN(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR),
    EJPHPM(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR),
    BBRHCJB(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR),
    BBRHPJB(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR),
    BBRHCJW(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR),
    BBRHPJW(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR),
    BBRHCJR(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR),
    BBRHPJR(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR),
    PEJP(ObisCode.UNKNOWN, UnitOfMeasurement.NONE),
    PTEC(ObisCode.UNKNOWN, UnitOfMeasurement.NONE),
    DEMAIN(ObisCode.UNKNOWN, UnitOfMeasurement.NONE),
    IINST(ObisCode.INSTANTANEOUS_CURRENT, UnitOfMeasurement.AMPERE),
    IINST1(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L1, UnitOfMeasurement.AMPERE),
    IINST2(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L2, UnitOfMeasurement.AMPERE),
    IINST3(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L3, UnitOfMeasurement.AMPERE),
    ADPS(ObisCode.UNKNOWN, UnitOfMeasurement.AMPERE),
    IMAX(ObisCode.MAXIMUM_CURRENT, UnitOfMeasurement.AMPERE),
    IMAX1(ObisCode.MAXIMUM_CURRENT_IN_PHASE_L1, UnitOfMeasurement.AMPERE),
    IMAX2(ObisCode.MAXIMUM_CURRENT_IN_PHASE_L2, UnitOfMeasurement.AMPERE),
    IMAX3(ObisCode.MAXIMUM_CURRENT_IN_PHASE_L3, UnitOfMeasurement.AMPERE),
    PMAX(ObisCode.UNKNOWN, UnitOfMeasurement.WATT),
    PAPP(ObisCode.APPARENT_INSTANTANEOUS_POWER, UnitOfMeasurement.VOLT_AMPERE),
    HHPHC(ObisCode.UNKNOWN, UnitOfMeasurement.NONE),
    MOTDETAT(ObisCode.UNKNOWN, UnitOfMeasurement.NONE),
    PPOT(ObisCode.UNKNOWN, UnitOfMeasurement.NONE);

    private static final Map<String, HistoryModeEntry> LOOK_UP_BY_ENTRY_KEY =
            Arrays.stream(HistoryModeEntry.values())
                  .collect(Collectors.toUnmodifiableMap(Enum::name,
                                                        Function.identity()));

    private final ObisCode obisCode;
    private final UnitOfMeasurement rawUnitOfMeasurement;

    HistoryModeEntry(ObisCode obisCode, UnitOfMeasurement rawUnitOfMeasurement) {
        this.obisCode = obisCode;
        this.rawUnitOfMeasurement = rawUnitOfMeasurement;
    }

    @JsonCreator
    public static HistoryModeEntry fromEntryKey(String entryKey) {
        var entry = LOOK_UP_BY_ENTRY_KEY.get(entryKey);
        if (entry == null) {
            throw new IllegalArgumentException("Unknown HistoryModeEntry key: " + entryKey);
        }

        return entry;
    }

    @JsonValue
    @Override
    public String toString() {
        return name();
    }

    public ObisCode obisCode() {
        return obisCode;
    }

    public UnitOfMeasurement rawUnitOfMeasurement() {
        return rawUnitOfMeasurement;
    }
}
