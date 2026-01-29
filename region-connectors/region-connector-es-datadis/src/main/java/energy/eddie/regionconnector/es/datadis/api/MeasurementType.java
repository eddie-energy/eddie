// SPDX-FileCopyrightText: 2023 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.api;

public enum MeasurementType {
    HOURLY(0),
    QUARTER_HOURLY(1);

    private final int value;

    MeasurementType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
