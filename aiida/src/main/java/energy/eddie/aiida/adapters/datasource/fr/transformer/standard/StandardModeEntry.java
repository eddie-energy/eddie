package energy.eddie.aiida.adapters.datasource.fr.transformer.standard;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import energy.eddie.aiida.models.record.UnitOfMeasurement;
import energy.eddie.aiida.utils.ObisCode;

import java.util.Arrays;

public enum StandardModeEntry {
    ADSC(ObisCode.DEVICE_ID_1, UnitOfMeasurement.NONE),
    VTIC(ObisCode.UNKNOWN, UnitOfMeasurement.NONE),
    DATE(ObisCode.UNKNOWN, UnitOfMeasurement.NONE),
    NGTF(ObisCode.UNKNOWN, UnitOfMeasurement.NONE),
    LTARF(ObisCode.UNKNOWN, UnitOfMeasurement.NONE),
    EAST(ObisCode.POSITIVE_ACTIVE_ENERGY, UnitOfMeasurement.WATT_HOUR),
    EASF01(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR),
    EASF02(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR),
    EASF03(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR),
    EASF04(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR),
    EASF05(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR),
    EASF06(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR),
    EASF07(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR),
    EASF08(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR),
    EASF09(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR),
    EASF10(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR),
    EASD01(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR),
    EASD02(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR),
    EASD03(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR),
    EASD04(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR),
    EAIT(ObisCode.NEGATIVE_ACTIVE_ENERGY, UnitOfMeasurement.WATT_HOUR),
    ERQ1(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE_REACTIVE_HOUR),
    ERQ2(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE_REACTIVE_HOUR),
    ERQ3(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE_REACTIVE_HOUR),
    ERQ4(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE_REACTIVE_HOUR),
    IRMS1(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L1, UnitOfMeasurement.AMPERE),
    IRMS2(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L2, UnitOfMeasurement.AMPERE),
    IRMS3(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L3, UnitOfMeasurement.AMPERE),
    URMS1(ObisCode.INSTANTANEOUS_VOLTAGE_IN_PHASE_L1, UnitOfMeasurement.VOLT),
    URMS2(ObisCode.INSTANTANEOUS_VOLTAGE_IN_PHASE_L2, UnitOfMeasurement.VOLT),
    URMS3(ObisCode.INSTANTANEOUS_VOLTAGE_IN_PHASE_L3, UnitOfMeasurement.VOLT),
    PREF(ObisCode.UNKNOWN, UnitOfMeasurement.KILO_VOLT_AMPERE),
    PCOUP(ObisCode.UNKNOWN, UnitOfMeasurement.KILO_VOLT_AMPERE),
    SINSTS(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE),
    SINSTS1(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE),
    SINSTS2(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE),
    SINSTS3(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE),
    SMAXSN(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE),
    SMAXSN1(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE),
    SMAXSN2(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE),
    SMAXSN3(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE),
    SMAXSN_1(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE),
    SMAXSN1_1(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE),
    SMAXSN2_1(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE),
    SMAXSN3_1(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE),
    SINSTI(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE),
    SMAXIN(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE),
    SMAXIN_1(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE),
    CCASN(ObisCode.UNKNOWN, UnitOfMeasurement.WATT),
    CCASN_1(ObisCode.UNKNOWN, UnitOfMeasurement.WATT),
    CCAIN(ObisCode.UNKNOWN, UnitOfMeasurement.WATT),
    CCAIN_1(ObisCode.UNKNOWN, UnitOfMeasurement.WATT),
    UMOY1(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT),
    UMOY2(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT),
    UMOY3(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT),
    STGE(ObisCode.UNKNOWN, UnitOfMeasurement.NONE),
    DPM1(ObisCode.UNKNOWN, UnitOfMeasurement.NONE),
    FPM1(ObisCode.UNKNOWN, UnitOfMeasurement.NONE),
    DPM2(ObisCode.UNKNOWN, UnitOfMeasurement.NONE),
    FPM2(ObisCode.UNKNOWN, UnitOfMeasurement.NONE),
    DPM3(ObisCode.UNKNOWN, UnitOfMeasurement.NONE),
    FPM3(ObisCode.UNKNOWN, UnitOfMeasurement.NONE),
    MSG1(ObisCode.UNKNOWN, UnitOfMeasurement.NONE),
    MSG2(ObisCode.UNKNOWN, UnitOfMeasurement.NONE),
    PRM(ObisCode.UNKNOWN, UnitOfMeasurement.NONE),
    RELAIS(ObisCode.UNKNOWN, UnitOfMeasurement.NONE),
    NTARF(ObisCode.UNKNOWN, UnitOfMeasurement.NONE),
    NJOURF(ObisCode.UNKNOWN, UnitOfMeasurement.NONE),
    NJOURF_1(ObisCode.UNKNOWN, UnitOfMeasurement.NONE),
    PJOURF_1(ObisCode.UNKNOWN, UnitOfMeasurement.NONE),
    PPOINTE(ObisCode.UNKNOWN, UnitOfMeasurement.NONE),
    ;

    private final ObisCode obisCode;
    private final UnitOfMeasurement rawUnitOfMeasurement;

    StandardModeEntry(ObisCode obisCode, UnitOfMeasurement rawUnitOfMeasurement) {
        this.obisCode = obisCode;
        this.rawUnitOfMeasurement = rawUnitOfMeasurement;
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

    public UnitOfMeasurement rawUnitOfMeasurement() {
        return rawUnitOfMeasurement;
    }
}
