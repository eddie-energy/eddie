// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors.conversion;

public class InvalidInstantOrDurationException extends Exception {
    public InvalidInstantOrDurationException(String value) {
        super("Could not parse Instant or Duration from '%s'".formatted(value));
    }
}
