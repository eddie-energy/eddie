package energy.eddie.regionconnector.de.eta.auth;

/**
 * Request DTO for ETA Plus Auth token exchange.
 * Note: ETA Plus uses a non-standard PUT method for token exchange.
 */
public record AuthTokenRequest(String token, String openid) {
}
