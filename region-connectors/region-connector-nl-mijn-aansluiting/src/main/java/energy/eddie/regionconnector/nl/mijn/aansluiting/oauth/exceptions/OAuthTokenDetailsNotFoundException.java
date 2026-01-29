// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.nl.mijn.aansluiting.oauth.exceptions;

public class OAuthTokenDetailsNotFoundException extends OAuthException {
    public OAuthTokenDetailsNotFoundException(String permissionId) {
        super("OAuthTokenDetails for permission ID %s not found".formatted(permissionId));
    }
}
