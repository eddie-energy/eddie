package energy.eddie.aiida.adapters.datasource.fr.transformer.standard;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import energy.eddie.aiida.models.record.UnitOfMeasurement;
import energy.eddie.aiida.utils.ObisCode;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum StandardModeEntry {
    ADSC(ObisCode.DEVICE_ID_1, UnitOfMeasurement.NONE, "ADSC", true),
    VTIC(ObisCode.UNKNOWN, UnitOfMeasurement.NONE, "VTIC", false),
    DATE(ObisCode.UNKNOWN, UnitOfMeasurement.NONE, "DATE", true),
    NGTF(ObisCode.UNKNOWN, UnitOfMeasurement.NONE, "NGTF", true),
    LTARF(ObisCode.UNKNOWN, UnitOfMeasurement.NONE, "LTARF", true),
    EAST(ObisCode.POSITIVE_ACTIVE_ENERGY, UnitOfMeasurement.WATT_HOUR, "EAST", false),
    EASF01(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR, "EASF01", false),
    EASF02(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR, "EASF02", false),
    EASF03(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR, "EASF03", false),
    EASF04(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR, "EASF04", false),
    EASF05(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR, "EASF05", false),
    EASF06(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR, "EASF06", false),
    EASF07(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR, "EASF07", false),
    EASF08(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR, "EASF08", false),
    EASF09(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR, "EASF09", false),
    EASF10(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR, "EASF10", false),
    EASD01(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR, "EASD01", false),
    EASD02(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR, "EASD02", false),
    EASD03(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR, "EASD03", false),
    EASD04(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR, "EASD04", false),
    EAIT(ObisCode.NEGATIVE_ACTIVE_ENERGY, UnitOfMeasurement.WATT_HOUR, "EAIT", false),
    ERQ1(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE_REACTIVE_HOUR, "ERQ1", false),
    ERQ2(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE_REACTIVE_HOUR, "ERQ2", false),
    ERQ3(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE_REACTIVE_HOUR, "ERQ3", false),
    ERQ4(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE_REACTIVE_HOUR, "ERQ4", false),
    IRMS1(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L1, UnitOfMeasurement.AMPERE, "IRMS1", false),
    IRMS2(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L2, UnitOfMeasurement.AMPERE, "IRMS2", false),
    IRMS3(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L3, UnitOfMeasurement.AMPERE, "IRMS3", false),
    URMS1(ObisCode.INSTANTANEOUS_VOLTAGE_IN_PHASE_L1, UnitOfMeasurement.VOLT, "URMS1", false),
    URMS2(ObisCode.INSTANTANEOUS_VOLTAGE_IN_PHASE_L2, UnitOfMeasurement.VOLT, "URMS2", false),
    URMS3(ObisCode.INSTANTANEOUS_VOLTAGE_IN_PHASE_L3, UnitOfMeasurement.VOLT, "URMS3", false),
    PREF(ObisCode.UNKNOWN, UnitOfMeasurement.KILO_VOLT_AMPERE, "PREF", true),
    PCOUP(ObisCode.UNKNOWN, UnitOfMeasurement.KILO_VOLT_AMPERE, "PCOUP", true),
    SINSTS(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE, "SINSTS", false),
    SINSTS1(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE, "SINSTS1", false),
    SINSTS2(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE, "SINSTS2", false),
    SINSTS3(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE, "SINSTS3", false),
    SMAXSN(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE, "SMAXSN", false),
    SMAXSN1(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE, "SMAXSN1", false),
    SMAXSN2(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE, "SMAXSN2", false),
    SMAXSN3(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE, "SMAXSN3", false),
    SMAXSN_1(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE, "SMAXSN-1", false),
    SMAXSN1_1(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE, "SMAXSN1-1", false),
    SMAXSN2_1(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE, "SMAXSN2-1", false),
    SMAXSN3_1(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE, "SMAXSN3-1", false),
    SINSTI(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE, "SINSTI", false),
    SMAXIN(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE, "SMAXIN", false),
    SMAXIN_1(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE, "SMAXIN-1", false),
    CCASN(ObisCode.UNKNOWN, UnitOfMeasurement.WATT, "CCASN", false),
    CCASN_1(ObisCode.UNKNOWN, UnitOfMeasurement.WATT, "CCASN-1", false),
    CCAIN(ObisCode.UNKNOWN, UnitOfMeasurement.WATT, "CCAIN", false),
    CCAIN_1(ObisCode.UNKNOWN, UnitOfMeasurement.WATT, "CCAIN-1", false),
    UMOY1(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT, "UMOY1", false),
    UMOY2(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT, "UMOY2", false),
    UMOY3(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT, "UMOY3", false),
    STGE(ObisCode.UNKNOWN, UnitOfMeasurement.NONE, "STGE", true),
    DPM1(ObisCode.UNKNOWN, UnitOfMeasurement.NONE, "DPM1", true),
    FPM1(ObisCode.UNKNOWN, UnitOfMeasurement.NONE, "FPM1", true),
    DPM2(ObisCode.UNKNOWN, UnitOfMeasurement.NONE, "DPM2", true),
    FPM2(ObisCode.UNKNOWN, UnitOfMeasurement.NONE, "FPM2", true),
    DPM3(ObisCode.UNKNOWN, UnitOfMeasurement.NONE, "DPM3", true),
    FPM3(ObisCode.UNKNOWN, UnitOfMeasurement.NONE, "FPM3", true),
    MSG1(ObisCode.UNKNOWN, UnitOfMeasurement.NONE, "MSG1", true),
    MSG2(ObisCode.UNKNOWN, UnitOfMeasurement.NONE, "MSG2", true),
    PRM(ObisCode.UNKNOWN, UnitOfMeasurement.NONE, "PRM", true),
    RELAIS(ObisCode.UNKNOWN, UnitOfMeasurement.NONE, "RELAIS", true),
    NTARF(ObisCode.UNKNOWN, UnitOfMeasurement.NONE, "NTARF", true),
    NJOURF(ObisCode.UNKNOWN, UnitOfMeasurement.NONE, "NJOURF", true),
    NJOURF_1(ObisCode.UNKNOWN, UnitOfMeasurement.NONE, "NJOURF+1", true),
    PJOURF_1(ObisCode.UNKNOWN, UnitOfMeasurement.NONE, "PJOURF+1", true),
    PPOINTE(ObisCode.UNKNOWN, UnitOfMeasurement.NONE, "PPOINTE", true);

    private static final Map<String, StandardModeEntry> LOOKUP_BY_RAW_ENTRY_KEY =
            Arrays.stream(StandardModeEntry.values())
                  .collect(Collectors.toUnmodifiableMap(
                          StandardModeEntry::rawEntryKey,
                          Function.identity()));
    private static final Set<String> KEYS_REQUIRING_SANITIZATION =
            Arrays.stream(StandardModeEntry.values())
                  .filter(StandardModeEntry::needsSanitization)
                  .map(StandardModeEntry::rawEntryKey)
                  .collect(Collectors.toUnmodifiableSet());

    private final ObisCode obisCode;
    private final UnitOfMeasurement rawUnitOfMeasurement;
    private final String rawEntryKey;
    private final boolean needsSanitization;

    StandardModeEntry(
            ObisCode obisCode,
            UnitOfMeasurement rawUnitOfMeasurement,
            String rawEntryKey,
            boolean needsSanitization
    ) {
        this.obisCode = obisCode;
        this.rawUnitOfMeasurement = rawUnitOfMeasurement;
        this.rawEntryKey = rawEntryKey;
        this.needsSanitization = needsSanitization;
    }

    @JsonCreator
    public static StandardModeEntry fromEntryKey(String entryKey) {
        var entry = LOOKUP_BY_RAW_ENTRY_KEY.get(entryKey);
        if (entry == null) {
            throw new IllegalArgumentException("Unknown StandardModeEntry key: " + entryKey);
        }

        return entry;
    }

    public static boolean needsSanitization(String key) {
        return KEYS_REQUIRING_SANITIZATION.contains(key);
    }

    public boolean needsSanitization() {
        return needsSanitization;
    }

    public String rawEntryKey() {
        return rawEntryKey;
    }

    @JsonValue
    @Override
    public String toString() {
        return rawEntryKey();
    }

    public ObisCode obisCode() {
        return obisCode;
    }

    public UnitOfMeasurement rawUnitOfMeasurement() {
        return rawUnitOfMeasurement;
    }
}
