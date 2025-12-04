package energy.eddie.regionconnector.de.eta.client;

/**
 * Signals a transient PA error (e.g., 5xx or network issue) so the caller can trigger retries.
 */
public class TransientPaException extends RuntimeException {
    public TransientPaException(String message) { super(message); }
    public TransientPaException(String message, Throwable cause) { super(message, cause); }
}
