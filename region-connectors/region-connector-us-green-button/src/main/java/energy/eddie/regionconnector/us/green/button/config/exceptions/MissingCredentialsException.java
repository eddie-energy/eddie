// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.config.exceptions;

public abstract class MissingCredentialsException extends Exception{
    protected MissingCredentialsException(String message, String company) {
        super(message.formatted(company));
    }
}
