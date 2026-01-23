// SPDX-FileCopyrightText: 2023-2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.xml.helper;

public enum Sector {
    ELECTRICITY("01"),
    GAS("02");

    private final String value;

    Sector(String value) {
        this.value = value;
    }

    public static Sector fromValue(String v) {
        return switch (v) {
            case "01" -> ELECTRICITY;
            case "02" -> GAS;
            default -> throw new IllegalArgumentException(v);
        };
    }

    public String value() {
        return value;
    }
}
