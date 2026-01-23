// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.dataneeds.exceptions;

public class DataNeedAlreadyExistsException extends Exception {
    public DataNeedAlreadyExistsException(String id) {
        super("Data need with ID '%s' already exists.".formatted(id));
    }
}