package energy.eddie.regionconnector.cds.services.oauth.token;

import java.time.ZonedDateTime;

public record Credentials(String accessToken, String refreshToken, ZonedDateTime expiresAt) implements TokenResult {
}
