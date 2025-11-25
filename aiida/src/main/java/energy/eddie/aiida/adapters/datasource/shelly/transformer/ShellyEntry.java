package energy.eddie.aiida.adapters.datasource.shelly.transformer;

import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.api.agnostic.aiida.UnitOfMeasurement;

import java.util.stream.Stream;

public enum ShellyEntry {
    TOTAL_ACTIVE_ENERGY(
            "total_act_energy",
            ObisCode.POSITIVE_ACTIVE_ENERGY,
            ObisCode.POSITIVE_ACTIVE_ENERGY_IN_PHASE_L1,
            ObisCode.POSITIVE_ACTIVE_ENERGY_IN_PHASE_L2,
            ObisCode.POSITIVE_ACTIVE_ENERGY_IN_PHASE_L3,
            UnitOfMeasurement.WATT_HOUR
    ),
    ACTIVE_ENERGY_EM(
            "act",
            ObisCode.POSITIVE_ACTIVE_ENERGY,
            ObisCode.POSITIVE_ACTIVE_ENERGY_IN_PHASE_L1,
            ObisCode.POSITIVE_ACTIVE_ENERGY_IN_PHASE_L2,
            ObisCode.POSITIVE_ACTIVE_ENERGY_IN_PHASE_L3,
            UnitOfMeasurement.WATT_HOUR
    ),
    ACTIVE_ENERGY_SWITCH("aenergy.total", ObisCode.POSITIVE_ACTIVE_ENERGY_IN_PHASE_L1, UnitOfMeasurement.WATT_HOUR),
    TOTAL_ACTIVE_RETURNED_ENERGY(
            "total_act_ret_energy",
            ObisCode.NEGATIVE_ACTIVE_ENERGY,
            ObisCode.NEGATIVE_ACTIVE_ENERGY_IN_PHASE_L1,
            ObisCode.NEGATIVE_ACTIVE_ENERGY_IN_PHASE_L2,
            ObisCode.NEGATIVE_ACTIVE_ENERGY_IN_PHASE_L3,
            UnitOfMeasurement.WATT_HOUR
    ),
    ACTIVE_RETURNED_ENERGY_EM(
            "act_ret",
            ObisCode.NEGATIVE_ACTIVE_ENERGY,
            ObisCode.NEGATIVE_ACTIVE_ENERGY_IN_PHASE_L1,
            ObisCode.NEGATIVE_ACTIVE_ENERGY_IN_PHASE_L2,
            ObisCode.NEGATIVE_ACTIVE_ENERGY_IN_PHASE_L3,
            UnitOfMeasurement.WATT_HOUR
    ),
    ACTIVE_RETURNED_ENERGY_SWITCH("ret_aenergy.total", ObisCode.NEGATIVE_ACTIVE_ENERGY_IN_PHASE_L1, UnitOfMeasurement.WATT_HOUR),
    CURRENT(
            "current",
            ObisCode.INSTANTANEOUS_CURRENT,
            ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_NEUTRAL,
            ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L1,
            ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L2,
            ObisCode.INSTANTANEOUS_CURRENT_IN_PHASE_L3,
            UnitOfMeasurement.AMPERE
    ),
    VOLTAGE(
            "voltage",
            ObisCode.INSTANTANEOUS_VOLTAGE,
            ObisCode.INSTANTANEOUS_VOLTAGE_IN_PHASE_L1,
            ObisCode.INSTANTANEOUS_VOLTAGE_IN_PHASE_L2,
            ObisCode.INSTANTANEOUS_VOLTAGE_IN_PHASE_L3,
            UnitOfMeasurement.VOLT
    ),
    ACTIVE_POWER_EM(
            "act_power",
            ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER,
            ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER_IN_PHASE_L1,
            ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER_IN_PHASE_L2,
            ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER_IN_PHASE_L3,
            UnitOfMeasurement.WATT
    ),
    ACTIVE_POWER_SWITCH("apower", ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER_IN_PHASE_L1, UnitOfMeasurement.WATT),
    APPARENT_POWER(
            "aprt_power",
            ObisCode.POSITIVE_REACTIVE_INSTANTANEOUS_POWER,
            ObisCode.POSITIVE_REACTIVE_INSTANTANEOUS_POWER_IN_PHASE_L1,
            ObisCode.POSITIVE_REACTIVE_INSTANTANEOUS_POWER_IN_PHASE_L2,
            ObisCode.POSITIVE_REACTIVE_INSTANTANEOUS_POWER_IN_PHASE_L3,
            UnitOfMeasurement.VOLT_AMPERE_REACTIVE
    ),
    POWER_FACTOR(
            "pf",
            ObisCode.INSTANTANEOUS_POWER_FACTOR,
            ObisCode.INSTANTANEOUS_POWER_FACTOR_IN_PHASE_L1,
            ObisCode.INSTANTANEOUS_POWER_FACTOR_IN_PHASE_L2,
            ObisCode.INSTANTANEOUS_POWER_FACTOR_IN_PHASE_L3,
            UnitOfMeasurement.NONE
    ),
    FREQUENCY("freq", ObisCode.FREQUENCY, UnitOfMeasurement.HERTZ),
    UNKNOWN("unknown", ObisCode.UNKNOWN, UnitOfMeasurement.UNKNOWN);

    private final String entrySuffix;
    private final ObisCode obisCodeTotal;
    private final ObisCode obisCodeNetral;
    private final ObisCode obisCodePhaseL1;
    private final ObisCode obisCodePhaseL2;
    private final ObisCode obisCodePhaseL3;
    private final UnitOfMeasurement rawUnitOfMeasurement;

    ShellyEntry(
            String entrySuffix,
            ObisCode obisCodeTotal,
            ObisCode obisCodeNetral,
            ObisCode obisCodePhaseL1,
            ObisCode obisCodePhaseL2,
            ObisCode obisCodePhaseL3,
            UnitOfMeasurement rawUnitOfMeasurement
    ) {
        this.entrySuffix = entrySuffix;
        this.obisCodeTotal = obisCodeTotal;
        this.obisCodeNetral = obisCodeNetral;
        this.obisCodePhaseL1 = obisCodePhaseL1;
        this.obisCodePhaseL2 = obisCodePhaseL2;
        this.obisCodePhaseL3 = obisCodePhaseL3;
        this.rawUnitOfMeasurement = rawUnitOfMeasurement;
    }

    ShellyEntry(
            String entrySuffix,
            ObisCode obisCodeTotal,
            ObisCode obisCodePhaseL1,
            ObisCode obisCodePhaseL2,
            ObisCode obisCodePhaseL3,
            UnitOfMeasurement rawUnitOfMeasurement
    ) {
        this(entrySuffix,
             obisCodeTotal,
             ObisCode.UNKNOWN,
             obisCodePhaseL1,
             obisCodePhaseL2,
             obisCodePhaseL3,
             rawUnitOfMeasurement);
    }

    ShellyEntry(String entrySuffix, ObisCode obisCode, UnitOfMeasurement rawUnitOfMeasurement) {
        this(entrySuffix, obisCode, obisCode, obisCode, obisCode, obisCode, rawUnitOfMeasurement);
    }

    public static ShellyEntry fromKey(String key) {
        return Stream.of(ShellyEntry.values())
                     .filter(entry -> key.endsWith(entry.entrySuffix))
                     .findFirst()
                     .orElse(ShellyEntry.UNKNOWN);
    }

    public ObisCode obisCodeForPhase(ShellyPhase phase) {
        return switch (phase) {
            case TOTAL -> obisCodeTotal;
            case NEUTRAL -> obisCodeNetral;
            case PHASE_L1 -> obisCodePhaseL1;
            case PHASE_L2 -> obisCodePhaseL2;
            case PHASE_L3 -> obisCodePhaseL3;
            case UNKNOWN -> ObisCode.UNKNOWN;
        };
    }

    public UnitOfMeasurement rawUnitOfMeasurement() {
        return rawUnitOfMeasurement;
    }
}
