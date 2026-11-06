// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors.datasource;

public class InvalidSimulationRangeException extends RuntimeException {
    public InvalidSimulationRangeException(String direction, long min, long max) {
        super("The minimum %s (%d W) must not be greater than the maximum %s (%d W).".formatted(direction,
                                                                                                min,
                                                                                                direction,
                                                                                                max));
    }
}
