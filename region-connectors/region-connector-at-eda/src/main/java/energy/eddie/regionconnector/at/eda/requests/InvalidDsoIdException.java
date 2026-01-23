// SPDX-FileCopyrightText: 2023 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.requests;

public final class InvalidDsoIdException extends Exception {
    public InvalidDsoIdException(String message) {
        super(message);
    }
}
