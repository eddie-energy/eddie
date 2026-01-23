// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.it.transformer;

import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.api.agnostic.aiida.UnitOfMeasurement;

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
    POSITIVE_ACTIVE_ENERGY(
            "1-0:1.8.0.255_3,0_2",
            ObisCode.POSITIVE_ACTIVE_ENERGY,
            UnitOfMeasurement.WATT_HOUR
    ),
    NEGATIVE_ACTIVE_ENERGY(
            "1-0:2.8.0.255_3,0_2",
            ObisCode.NEGATIVE_ACTIVE_ENERGY,
            UnitOfMeasurement.WATT_HOUR
    ),
    DEVICE_ID_1(
            "0-0:96.1.0.255_1,0_2",
            ObisCode.DEVICE_ID_1,
            UnitOfMeasurement.NONE
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
