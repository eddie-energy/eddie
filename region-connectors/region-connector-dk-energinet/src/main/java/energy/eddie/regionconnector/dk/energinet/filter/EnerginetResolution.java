// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.dk.energinet.filter;

public record EnerginetResolution(String resolution) {

    /**
     * Converts the resolution returned by Energinet to ISO 8601 compliant duration format.
     *
     * @return the resolution in ISO 8601 duration format
     */
    public String toISO8601Duration() {
        return switch (resolution) {
            case "PT1D" -> "P1D";
            case "PT1Y" -> "P1Y";
            default -> resolution;
        };
    }
}
