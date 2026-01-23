// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.services.oauth.token;

import java.time.ZonedDateTime;

public record CredentialsWithRefreshToken(String accessToken, String refreshToken, ZonedDateTime expiresAt) implements TokenResult {
}
