package energy.eddie.aiida.adapters.datasource.it.transformer;

import energy.eddie.aiida.models.record.UnitOfMeasurement;
import energy.eddie.aiida.utils.ObisCode;

import java.util.stream.Stream;

public enum SinapsiAlfaEntry {
    POSITIVE_ACTIVE_INSTANTANEOUS_POWER(
            "1-0:1.7.0.255_3,0_2",
            ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER,
            UnitOfMeasurement.WATT
    ),
    NEGATIVE_ACTIVE_INSTANTANEOUS_POWER(
            "1-0:2.7.0.255_3,0_2",
            ObisCode.NEGATIVE_ACTIVE_INSTANTANEOUS_POWER,
            UnitOfMeasurement.WATT
    ),
    POWER_THRESHOLD_CROSSING_EVENT_COUNTER(
            "1-0:16.7.0.255_3,0_2",
            ObisCode.UNKNOWN,
            UnitOfMeasurement.NONE
    ),
    POSITIVE_ACTIVE_ENERGY(
            "1-0:1.8.0.255_3,0_2",
            ObisCode.POSITIVE_ACTIVE_ENERGY,
            UnitOfMeasurement.WATT_HOUR
    ),
    AVERAGE_POSITIVE_ACTIVE_POWER_OVER_15_MINUTES(
            "1-0:1.27.0.255_3,0_2",
            ObisCode.UNKNOWN,
            UnitOfMeasurement.WATT
    ),
    QUARTERLY_PROFILE_OF_POSITIVE_ACTIVE_ENERGY_LAST_SAMPLE(
            "1-0:99.1.0.255_7,1_2___0",
            ObisCode.UNKNOWN,
            UnitOfMeasurement.WATT_HOUR
    ),
    QUARTERLY_PROFILE_OF_POSITIVE_ACTIVE_ENERGY_PREVIOUS_SAMPLE(
            "1-0:99.1.0.255_7,1_2___1",
            ObisCode.UNKNOWN,
            UnitOfMeasurement.WATT_HOUR
    ),
    UNKNOWN("unknown", ObisCode.UNKNOWN, UnitOfMeasurement.UNKNOWN);

    private final String entryKey;
    private final ObisCode obisCode;
    private final UnitOfMeasurement rawUnitOfMeasurement;

    SinapsiAlfaEntry(
            String entryKey,
            ObisCode obisCode,
            UnitOfMeasurement rawUnitOfMeasurement
    ) {
        this.entryKey = entryKey;
        this.obisCode = obisCode;
        this.rawUnitOfMeasurement = rawUnitOfMeasurement;
    }

    public static SinapsiAlfaEntry fromKey(String key) {
        return Stream.of(SinapsiAlfaEntry.values())
                     .filter(entry -> key.equals(entry.entryKey))
                     .findFirst()
                     .orElse(SinapsiAlfaEntry.UNKNOWN);
    }

    public ObisCode obisCode() {
        return obisCode;
    }

    public UnitOfMeasurement rawUnitOfMeasurement() {
        return rawUnitOfMeasurement;
    }
}
