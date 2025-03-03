package energy.eddie.regionconnector.cds.services.oauth.token;

import java.time.ZonedDateTime;

public record CredentialsWithRefreshToken(String accessToken, String refreshToken, ZonedDateTime expiresAt) implements TokenResult {
}
