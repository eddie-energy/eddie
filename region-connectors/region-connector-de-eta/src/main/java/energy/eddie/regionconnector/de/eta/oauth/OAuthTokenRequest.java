package energy.eddie.regionconnector.de.eta.oauth;

/**
 * Request DTO for ETA Plus OAuth token exchange.
 * Note: ETA Plus uses a non-standard PUT method for token exchange.
 */
public record OAuthTokenRequest(String token, String openid) {
}
