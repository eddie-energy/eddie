// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.nl.mijn.aansluiting.exceptions;

import energy.eddie.api.agnostic.process.model.validation.AttributeError;

public class NlValidationException extends Exception {
    @SuppressWarnings("java:S1948") // False positive
    private final AttributeError error;

    public NlValidationException(AttributeError error) {this.error = error;}

    public AttributeError error() {
        return error;
    }
}
