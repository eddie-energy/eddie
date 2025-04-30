package energy.eddie.aiida.adapters.datasource.fr.transformer.history;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import energy.eddie.aiida.models.record.UnitOfMeasurement;
import energy.eddie.aiida.utils.ObisCode;

import java.util.Arrays;

public enum HistoryModeEntry {
    MOTDETAT(ObisCode.UNKNOWN, UnitOfMeasurement.NONE),
    ADCO(ObisCode.METER_SERIAL, UnitOfMeasurement.NONE),
    OPTARIF(ObisCode.UNKNOWN, UnitOfMeasurement.NONE),
    ISOUSC(ObisCode.UNKNOWN, UnitOfMeasurement.AMPERE),
    BASE(ObisCode.POSITIVE_ACTIVE_ENERGY, UnitOfMeasurement.WATT_HOUR),
    PTEC(ObisCode.UNKNOWN, UnitOfMeasurement.NONE),
    IINST(ObisCode.INSTANTANEOUS_CURRENT, UnitOfMeasurement.AMPERE),
    IINST1(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L1, UnitOfMeasurement.AMPERE),
    IINST2(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L2, UnitOfMeasurement.AMPERE),
    IINST3(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L3, UnitOfMeasurement.AMPERE),
    IMAX(ObisCode.MAXIMUM_CURRENT, UnitOfMeasurement.AMPERE),
    IMAX1(ObisCode.MAXIMUM_CURRENT_IN_PHASE_L1, UnitOfMeasurement.AMPERE),
    IMAX2(ObisCode.MAXIMUM_CURRENT_IN_PHASE_L2, UnitOfMeasurement.AMPERE),
    IMAX3(ObisCode.MAXIMUM_CURRENT_IN_PHASE_L3, UnitOfMeasurement.AMPERE),
    PAPP(ObisCode.APPARENT_INSTANTANEOUS_POWER, UnitOfMeasurement.VOLT_AMPERE),
    HHPHC(ObisCode.UNKNOWN, UnitOfMeasurement.NONE);

    private final ObisCode obisCode;
    private final UnitOfMeasurement rawUnitOfMeasurement;

    HistoryModeEntry(ObisCode obisCode, UnitOfMeasurement rawUnitOfMeasurement) {
        this.obisCode = obisCode;
        this.rawUnitOfMeasurement = rawUnitOfMeasurement;
    }

    @JsonCreator
    public static HistoryModeEntry fromEntryKey(String entryKey) {
        return Arrays.stream(HistoryModeEntry.values())
                     .filter(entry -> entry.name().equals(entryKey))
                     .findFirst()
                     .orElseThrow();
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
