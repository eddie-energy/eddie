package energy.eddie.regionconnector.cds.services.oauth.token;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public record CredentialsWithoutRefreshToken(String accessToken, ZonedDateTime expiresAt) implements TokenResult {
    public boolean isValid() {
        return ZonedDateTime.now(ZoneOffset.UTC).isBefore(expiresAt);
    }
}
