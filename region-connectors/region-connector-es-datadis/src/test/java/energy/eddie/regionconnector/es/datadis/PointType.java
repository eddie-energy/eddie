// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis;

/**
 * Indicates in which granularity a meter can provide metering data
 */
public enum PointType {
    /**
     * Type 1 can provide hourly or quarter hourly metering data
     */
    TYPE_1(1),
    /**
     * Type 2 can provide hourly or quarter hourly metering data
     */
    TYPE_2(2),
    /**
     * Type 3 can provide hourly  metering data
     */
    TYPE_3(3),
    /**
     * Type 4 can provide hourly metering data
     */
    TYPE_4(4),
    /**
     * Type 5 can provide hourly metering data
     */
    TYPE_5(5);
    private final int value;

    PointType(int value) {
        this.value = value;
    }

    public int value() {return value;}
}
