// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers &lt;eddie.developers@fh-hagenberg.at&gt;
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.de.eta.oauth;

/**
 * Request DTO for ETA Plus OAuth token exchange.
 * Note: ETA Plus uses a non-standard PUT method for token exchange.
 */
public record OAuthTokenRequest(String token, String openid) {
}
