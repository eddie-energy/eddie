// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.timeout;

public class InvalidTimeoutConfigurationException extends RuntimeException {
    public InvalidTimeoutConfigurationException(String message) {
        super(message);
    }
}
