// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.oauth;

public class OAuthRequestException extends Exception {
    public OAuthRequestException(Exception e) {
        super(e);
    }
}
