package energy.eddie.regionconnector.de.eta.oauth;

/**
 * Custom exception for DE-ETA OAuth related failures.
 */
public class DeEtaOAuthException extends RuntimeException {
    public DeEtaOAuthException(String message) { super(message); }
    public DeEtaOAuthException(String message, Throwable cause) { super(message, cause); }
}
