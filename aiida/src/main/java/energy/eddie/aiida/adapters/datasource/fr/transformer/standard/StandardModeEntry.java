// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.fr.transformer.standard;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.api.agnostic.aiida.UnitOfMeasurement;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum StandardModeEntry {
    /**
     * Meter's secondary address (None)
     */
    ADSC(ObisCode.METER_SERIAL, UnitOfMeasurement.NONE, "ADSC", true),
    /** TIC version (None) */
    VTIC(ObisCode.UNKNOWN, UnitOfMeasurement.NONE, "VTIC", false),
    /** Current date and time (None) */
    DATE(ObisCode.UNKNOWN, UnitOfMeasurement.NONE, "DATE", true),
    /** Name of supplier's tariff calendar (None) */
    NGTF(ObisCode.UNKNOWN, UnitOfMeasurement.NONE, "NGTF", true),
    /** Current supplier's tariff label (None) */
    LTARF(ObisCode.UNKNOWN, UnitOfMeasurement.NONE, "LTARF", true),
    /** Total extracted active energy (Wh) */
    EAST(ObisCode.POSITIVE_ACTIVE_ENERGY, UnitOfMeasurement.WATT_HOUR, "EAST", false),
    /** Supplier extracted active energy, index 01 (Wh) */
    EASF01(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR, "EASF01", false),
    /** Supplier extracted active energy, index 02 (Wh) */
    EASF02(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR, "EASF02", false),
    /** Supplier extracted active energy, index 03 (Wh) */
    EASF03(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR, "EASF03", false),
    /** Supplier extracted active energy, index 04 (Wh) */
    EASF04(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR, "EASF04", false),
    /** Supplier extracted active energy, index 05 (Wh) */
    EASF05(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR, "EASF05", false),
    /** Supplier extracted active energy, index 06 (Wh) */
    EASF06(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR, "EASF06", false),
    /** Supplier extracted active energy, index 07 (Wh) */
    EASF07(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR, "EASF07", false),
    /** Supplier extracted active energy, index 08 (Wh) */
    EASF08(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR, "EASF08", false),
    /** Supplier extracted active energy, index 09 (Wh) */
    EASF09(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR, "EASF09", false),
    /** Supplier extracted active energy, index 10 (Wh) */
    EASF10(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR, "EASF10", false),
    /** Distributor extracted active energy, index 01 (Wh) */
    EASD01(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR, "EASD01", false),
    /** Distributor extracted active energy, index 02 (Wh) */
    EASD02(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR, "EASD02", false),
    /** Distributor extracted active energy, index 03 (Wh) */
    EASD03(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR, "EASD03", false),
    /** Distributor extracted active energy, index 04 (Wh) */
    EASD04(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR, "EASD04", false),
    /** Total injected active energy (Wh) */
    EAIT(ObisCode.NEGATIVE_ACTIVE_ENERGY, UnitOfMeasurement.WATT_HOUR, "EAIT", false),
    /** Total Q1 reactive energy (VArh) */
    ERQ1(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE_REACTIVE_HOUR, "ERQ1", false),
    /** Total Q2 reactive energy (VArh) */
    ERQ2(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE_REACTIVE_HOUR, "ERQ2", false),
    /** Total Q3 reactive energy (VArh) */
    ERQ3(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE_REACTIVE_HOUR, "ERQ3", false),
    /** Total Q4 reactive energy (VArh) */
    ERQ4(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE_REACTIVE_HOUR, "ERQ4", false),
    /** Effective current, phase 1 (A) */
    IRMS1(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L1, UnitOfMeasurement.AMPERE, "IRMS1", false),
    /** Effective current, phase 2 (A) */
    IRMS2(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L2, UnitOfMeasurement.AMPERE, "IRMS2", false),
    /** Effective current, phase 3 (A) */
    IRMS3(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L3, UnitOfMeasurement.AMPERE, "IRMS3", false),
    /** Effective voltage, phase 1 (V) */
    URMS1(ObisCode.INSTANTANEOUS_VOLTAGE_IN_PHASE_L1, UnitOfMeasurement.VOLT, "URMS1", false),
    /** Effective voltage, phase 2 (V) */
    URMS2(ObisCode.INSTANTANEOUS_VOLTAGE_IN_PHASE_L2, UnitOfMeasurement.VOLT, "URMS2", false),
    /** Effective voltage, phase 3 (V) */
    URMS3(ObisCode.INSTANTANEOUS_VOLTAGE_IN_PHASE_L3, UnitOfMeasurement.VOLT, "URMS3", false),
    /** App. reference power (PREF) (kVA) */
    PREF(ObisCode.UNKNOWN, UnitOfMeasurement.KILO_VOLT_AMPERE, "PREF", true),
    /** App. breaking capacity (PCOUP) (kVA) */
    PCOUP(ObisCode.UNKNOWN, UnitOfMeasurement.KILO_VOLT_AMPERE, "PCOUP", true),
    /** Extracted instantaneous app. power (VA) */
    SINSTS(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE, "SINSTS", false),
    /** Extracted instantaneous app. power phase 1 (VA) */
    SINSTS1(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE, "SINSTS1", false),
    /** Extracted instantaneous app. power phase 2 (VA) */
    SINSTS2(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE, "SINSTS2", false),
    /** Extracted instantaneous app. power phase 3 (VA) */
    SINSTS3(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE, "SINSTS3", false),
    /** Extracted max. app. power n (VA) */
    SMAXSN(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE, "SMAXSN", false),
    /** Extracted max. app. power n phase 1 (VA) */
    SMAXSN1(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE, "SMAXSN1", false),
    /** Extracted max. app. power n phase 2 (VA) */
    SMAXSN2(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE, "SMAXSN2", false),
    /** Extracted max. app. power n phase 3 (VA) */
    SMAXSN3(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE, "SMAXSN3", false),
    /** Extracted max. app. power n-1 (VA) */
    SMAXSN_1(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE, "SMAXSN-1", false),
    /** Extracted max. app. power n-1 phase 1 (VA) */
    SMAXSN1_1(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE, "SMAXSN1-1", false),
    /** Extracted max. app. power n-1 phase 2 (VA) */
    SMAXSN2_1(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE, "SMAXSN2-1", false),
    /** Extracted max. app. power n-1 phase 3 (VA) */
    SMAXSN3_1(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE, "SMAXSN3-1", false),
    /** Injected instantaneous app. power (VA) */
    SINSTI(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE, "SINSTI", false),
    /** Injected max. app. power n (VA) */
    SMAXIN(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE, "SMAXIN", false),
    /** Injected max. app. power n-1 (VA) */
    SMAXIN_1(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT_AMPERE, "SMAXIN-1", false),
    /** Point n of the extracted active load curve (W) */
    CCASN(ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER, UnitOfMeasurement.WATT, "CCASN", false),
    /** Point n-1 of the extracted active load curve (W) */
    CCASN_1(ObisCode.UNKNOWN, UnitOfMeasurement.WATT, "CCASN-1", false),
    /** Point n of the injected active load curve (W) */
    CCAIN(ObisCode.NEGATIVE_ACTIVE_INSTANTANEOUS_POWER, UnitOfMeasurement.WATT, "CCAIN", false),
    /** Point n-1 of the injected active load curve (W) */
    CCAIN_1(ObisCode.UNKNOWN, UnitOfMeasurement.WATT, "CCAIN-1", false),
    /** Mean voltage ph. 1 (V) */
    UMOY1(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT, "UMOY1", false),
    /** Mean voltage ph. 2 (V) */
    UMOY2(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT, "UMOY2", false),
    /** Mean voltage ph. 3 (V) */
    UMOY3(ObisCode.UNKNOWN, UnitOfMeasurement.VOLT, "UMOY3", false),
    /** Status Register (None) */
    STGE(ObisCode.UNKNOWN, UnitOfMeasurement.NONE, "STGE", true),
    /** Start of Mobile 1 Peak Time (None) */
    DPM1(ObisCode.UNKNOWN, UnitOfMeasurement.NONE, "DPM1", true),
    /** End of Mobile 1 Peak Time (None) */
    FPM1(ObisCode.UNKNOWN, UnitOfMeasurement.NONE, "FPM1", true),
    /** Start of Mobile 2 Peak Time (None) */
    DPM2(ObisCode.UNKNOWN, UnitOfMeasurement.NONE, "DPM2", true),
    /** End of Mobile 2 Peak Time (None) */
    FPM2(ObisCode.UNKNOWN, UnitOfMeasurement.NONE, "FPM2", true),
    /** Start of Mobile 3 Peak Time (None) */
    DPM3(ObisCode.UNKNOWN, UnitOfMeasurement.NONE, "DPM3", true),
    /** End of Mobile 3 Peak Time (None) */
    FPM3(ObisCode.UNKNOWN, UnitOfMeasurement.NONE, "FPM3", true),
    /** Short message (None) */
    MSG1(ObisCode.UNKNOWN, UnitOfMeasurement.NONE, "MSG1", true),
    /** Ultra-short message (None) */
    MSG2(ObisCode.UNKNOWN, UnitOfMeasurement.NONE, "MSG2", true),
    /** PRM (None) */
    PRM(ObisCode.DEVICE_ID_1, UnitOfMeasurement.NONE, "PRM", true),
    /** Relay (None) */
    RELAIS(ObisCode.UNKNOWN, UnitOfMeasurement.NONE, "RELAIS", true),
    /** Current tariff index number (None) */
    NTARF(ObisCode.UNKNOWN, UnitOfMeasurement.NONE, "NTARF", true),
    /** Number of current day in supplier's calendar (None) */
    NJOURF(ObisCode.UNKNOWN, UnitOfMeasurement.NONE, "NJOURF", true),
    /** Number of next day in supplier's calendar (None) */
    NJOURF_1(ObisCode.UNKNOWN, UnitOfMeasurement.NONE, "NJOURF+1", true),
    /** Profile of the next day in the supplier's calendar (None) */
    PJOURF_1(ObisCode.UNKNOWN, UnitOfMeasurement.NONE, "PJOURF+1", true),
    /** Point n of the extracted active load curve (W) */
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
