package energy.eddie.aiida.adapters.datasource.fr.transformer.standard;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import energy.eddie.aiida.utils.ObisCode;

import java.util.Arrays;

public enum StandardModeEntry {
    ADSC(ObisCode.DEVICE_ID_1),
    VTIC(ObisCode.UNKNOWN),
    DATE(ObisCode.UNKNOWN),
    NGTF(ObisCode.UNKNOWN),
    LTARF(ObisCode.UNKNOWN),
    EAST(ObisCode.POSITIVE_ACTIVE_ENERGY),
    EASF01(ObisCode.UNKNOWN),
    EASF02(ObisCode.UNKNOWN),
    EASF03(ObisCode.UNKNOWN),
    EASF04(ObisCode.UNKNOWN),
    EASF05(ObisCode.UNKNOWN),
    EASF06(ObisCode.UNKNOWN),
    EASF07(ObisCode.UNKNOWN),
    EASF08(ObisCode.UNKNOWN),
    EASF09(ObisCode.UNKNOWN),
    EASF10(ObisCode.UNKNOWN),
    EASD01(ObisCode.UNKNOWN),
    EASD02(ObisCode.UNKNOWN),
    EASD03(ObisCode.UNKNOWN),
    EASD04(ObisCode.UNKNOWN),
    EAIT(ObisCode.NEGATIVE_ACTIVE_ENERGY),
    ERQ1(ObisCode.UNKNOWN),
    ERQ2(ObisCode.UNKNOWN),
    ERQ3(ObisCode.UNKNOWN),
    ERQ4(ObisCode.UNKNOWN),
    IRMS1(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L1),
    IRMS2(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L2),
    IRMS3(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L3),
    URMS1(ObisCode.INSTANTANEOUS_VOLTAGE_IN_PHASE_L1),
    URMS2(ObisCode.INSTANTANEOUS_VOLTAGE_IN_PHASE_L2),
    URMS3(ObisCode.INSTANTANEOUS_VOLTAGE_IN_PHASE_L3),
    PREF(ObisCode.UNKNOWN),
    PCOUP(ObisCode.UNKNOWN),
    SINSTS(ObisCode.UNKNOWN),
    SINSTS1(ObisCode.UNKNOWN),
    SINSTS2(ObisCode.UNKNOWN),
    SINSTS3(ObisCode.UNKNOWN),
    SMAXSN(ObisCode.UNKNOWN),
    SMAXSN1(ObisCode.UNKNOWN),
    SMAXSN2(ObisCode.UNKNOWN),
    SMAXSN3(ObisCode.UNKNOWN),
    SMAXSN_1(ObisCode.UNKNOWN),
    SMAXSN1_1(ObisCode.UNKNOWN),
    SMAXSN2_1(ObisCode.UNKNOWN),
    SMAXSN3_1(ObisCode.UNKNOWN),
    SINSTI(ObisCode.UNKNOWN),
    SMAXIN(ObisCode.UNKNOWN),
    SMAXIN_1(ObisCode.UNKNOWN),
    CCASN(ObisCode.UNKNOWN),
    CCASN_1(ObisCode.UNKNOWN),
    CCAIN(ObisCode.UNKNOWN),
    CCAIN_1(ObisCode.UNKNOWN),
    UMOY1(ObisCode.UNKNOWN),
    UMOY2(ObisCode.UNKNOWN),
    UMOY3(ObisCode.UNKNOWN),
    STGE(ObisCode.UNKNOWN),
    DPM1(ObisCode.UNKNOWN),
    FPM1(ObisCode.UNKNOWN),
    DPM2(ObisCode.UNKNOWN),
    FPM2(ObisCode.UNKNOWN),
    DPM3(ObisCode.UNKNOWN),
    FPM3(ObisCode.UNKNOWN),
    MSG1(ObisCode.UNKNOWN),
    MSG2(ObisCode.UNKNOWN),
    PRM(ObisCode.UNKNOWN),
    RELAIS(ObisCode.UNKNOWN),
    NTARF(ObisCode.UNKNOWN),
    NJOURF(ObisCode.UNKNOWN),
    NJOURF_1(ObisCode.UNKNOWN),
    PJOURF_1(ObisCode.UNKNOWN),
    PPOINTE(ObisCode.UNKNOWN);

    private final ObisCode obisCode;

    StandardModeEntry(ObisCode obisCode) {
        this.obisCode = obisCode;
    }

    @JsonCreator
    public static StandardModeEntry fromEntryKey(String entryKey) {
        return Arrays.stream(StandardModeEntry.values())
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
