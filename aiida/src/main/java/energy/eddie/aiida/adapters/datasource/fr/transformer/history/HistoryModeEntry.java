package energy.eddie.aiida.adapters.datasource.fr.transformer.history;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import energy.eddie.aiida.utils.ObisCode;

import java.util.Arrays;

public enum HistoryModeEntry {
    MOTDETAT(ObisCode.UNKNOWN),
    ADCO(ObisCode.METER_SERIAL),
    OPTARIF(ObisCode.UNKNOWN),
    ISOUSC(ObisCode.UNKNOWN),
    BASE(ObisCode.POSITIVE_ACTIVE_ENERGY),
    PTEC(ObisCode.UNKNOWN),
    IINST(ObisCode.INSTANTANEOUS_CURRENT),
    IINST1(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L1),
    IINST2(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L2),
    IINST3(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L3),
    IMAX(ObisCode.MAXIMUM_CURRENT),
    IMAX1(ObisCode.MAXIMUM_CURRENT_IN_PHASE_L1),
    IMAX2(ObisCode.MAXIMUM_CURRENT_IN_PHASE_L2),
    IMAX3(ObisCode.MAXIMUM_CURRENT_IN_PHASE_L3),
    PAPP(ObisCode.APPARENT_INSTANTANEOUS_POWER),
    HHPHC(ObisCode.UNKNOWN);
    
    private final ObisCode obisCode;

    HistoryModeEntry(ObisCode obisCode) {
        this.obisCode = obisCode;
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
}
