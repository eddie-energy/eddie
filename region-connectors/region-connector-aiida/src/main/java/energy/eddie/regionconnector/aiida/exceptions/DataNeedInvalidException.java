// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.exceptions;

public class DataNeedInvalidException extends Exception {
    public DataNeedInvalidException(String dataNeedId, String message) {
        super("Data need with ID '%s' is invalid: %s".formatted(dataNeedId, message));
    }
}
