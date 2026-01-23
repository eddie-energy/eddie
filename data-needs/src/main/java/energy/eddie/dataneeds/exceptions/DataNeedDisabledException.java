// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.dataneeds.exceptions;

public class DataNeedDisabledException extends Exception {
    public DataNeedDisabledException(String dataNeedId) {
        super("Data need with ID '%s' is disabled.".formatted(dataNeedId));
    }
}
