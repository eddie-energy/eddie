// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.providers;

public class UnsupportedUnitException extends Exception {
    public UnsupportedUnitException(String unitOfMeasurement) {
        super("Unsupported unit: " + unitOfMeasurement);
    }
}
