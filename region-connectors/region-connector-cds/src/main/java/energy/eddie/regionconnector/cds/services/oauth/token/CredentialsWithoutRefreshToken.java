// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.services.oauth.token;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public record CredentialsWithoutRefreshToken(String accessToken, ZonedDateTime expiresAt) implements TokenResult {
    public boolean isValid() {
        return ZonedDateTime.now(ZoneOffset.UTC).isBefore(expiresAt);
    }
}
