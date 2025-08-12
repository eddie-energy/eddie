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
    /**
     * Meter’s address (12 chars, ADS)
     */
    ADCO(ObisCode.METER_SERIAL, UnitOfMeasurement.NONE),
    /** Chosen tariff option (4 chars, e.g. "BASE") */
    OPTARIF(ObisCode.UNKNOWN, UnitOfMeasurement.NONE),
    /** Subscribed intensity (2 chars, A; PREF in VA / 200 V) */
    ISOUSC(ObisCode.UNKNOWN, UnitOfMeasurement.AMPERE),
    /** Base index option (9 chars, Wh; totaliser index) */
    BASE(ObisCode.POSITIVE_ACTIVE_ENERGY, UnitOfMeasurement.WATT_HOUR),
    /** Off-peak hours index (9 chars, Wh; Supplier 1) */
    HCHC(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR),
    /** Peak hours index (9 chars, Wh; Supplier 2) */
    HCHP(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR),
    /** EJP index, normal times (9 chars, Wh; Supplier 1) */
    EJPHN(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR),
    /** EJP index, mobile peak times (9 chars, Wh; Supplier 2) */
    EJPHPM(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR),
    /** Tempo off-peak, blue days (9 chars, Wh; Supplier 1) */
    BBRHCJB(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR),
    /** Tempo peak, blue days (9 chars, Wh; Supplier 2) */
    BBRHPJB(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR),
    /** Tempo off-peak, white days (9 chars, Wh; Supplier 3) */
    BBRHCJW(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR),
    /** Tempo peak, white days (9 chars, Wh; Supplier 4) */
    BBRHPJW(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR),
    /** Tempo off-peak, red days (9 chars, Wh; Supplier 5) */
    BBRHCJR(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR),
    /** Tempo peak, red days (9 chars, Wh; Supplier 6) */
    BBRHPJR(ObisCode.UNKNOWN, UnitOfMeasurement.WATT_HOUR),
    /** EJP start notification (2 chars, min; "30" = 30 minutes before) */
    PEJP(ObisCode.UNKNOWN, UnitOfMeasurement.NONE),
    /** Current tariff period (4 chars; e.g. "TH..") */
    PTEC(ObisCode.UNKNOWN, UnitOfMeasurement.NONE),
    /** Tomorrow’s colour in Tempo mode (4 chars) */
    DEMAIN(ObisCode.UNKNOWN, UnitOfMeasurement.NONE),
    /** Instantaneous current (3 chars, A; single-phase only) */
    IINST(ObisCode.INSTANTANEOUS_CURRENT, UnitOfMeasurement.AMPERE),
    /** Instantaneous current in phase 1 (3 chars, A; three-phase only) */
    IINST1(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L1, UnitOfMeasurement.AMPERE),
    /** Instantaneous current in phase 2 (3 chars, A; three-phase only) */
    IINST2(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L2, UnitOfMeasurement.AMPERE),
    /** Instantaneous current in phase 3 (3 chars, A; three-phase only) */
    IINST3(ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L3, UnitOfMeasurement.AMPERE),
    /** Subscribed power exceedance notification (3 chars, A; single-phase only; triggered if IINST > RI) */
    ADPS(ObisCode.UNKNOWN, UnitOfMeasurement.AMPERE),
    /** Maximum intensity called (3 chars, A; single-phase only, e.g. 90 A) */
    IMAX(ObisCode.MAXIMUM_CURRENT, UnitOfMeasurement.AMPERE),
    /** Maximum intensity in phase 1 (3 chars, A; three-phase only) */
    IMAX1(ObisCode.MAXIMUM_CURRENT_IN_PHASE_L1, UnitOfMeasurement.AMPERE),
    /** Maximum intensity in phase 2 (3 chars, A; three-phase only) */
    IMAX2(ObisCode.MAXIMUM_CURRENT_IN_PHASE_L2, UnitOfMeasurement.AMPERE),
    /** Maximum intensity in phase 3 (3 chars, A; three-phase only) */
    IMAX3(ObisCode.MAXIMUM_CURRENT_IN_PHASE_L3, UnitOfMeasurement.AMPERE),
    /** Maximum three-phase power reached (5 chars, W; Smax of day n-1; three-phase only) */
    PMAX(ObisCode.UNKNOWN, UnitOfMeasurement.WATT),
    /** Apparent power (5 chars, VA; rounded to nearest 10 VA) */
    PAPP(ObisCode.APPARENT_INSTANTANEOUS_POWER, UnitOfMeasurement.VOLT_AMPERE),
    /** Peak/Off-peak indicator (1 char; e.g. "A") */
    HHPHC(ObisCode.UNKNOWN, UnitOfMeasurement.NONE),
    /** Meter status word (6 chars; e.g. "000000") */
    MOTDETAT(ObisCode.UNKNOWN, UnitOfMeasurement.NONE),
    /** Presence of potential; "0X" where bit n = 1 means phase n is absent (three-phase only) */
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
