// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.config.exceptions;

public class MissingClientIdException extends MissingCredentialsException {
    private static final String MESSAGE = "No client id found for the given utility %s. Please check the configuration.";

    public MissingClientIdException(String company) {
        super(MESSAGE, company);
    }
}
