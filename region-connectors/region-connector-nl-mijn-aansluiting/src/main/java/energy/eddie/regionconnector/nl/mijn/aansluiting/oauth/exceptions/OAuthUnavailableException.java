// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.nl.mijn.aansluiting.oauth.exceptions;

public class OAuthUnavailableException extends Exception {
    public OAuthUnavailableException(Exception cause) {
        super(cause);
    }
}
