package energy.eddie.spring;

/**
 * Exceptions which may indicate that some configurations are missing or invalid.
 * The message contains more details.
 */
public final class InitializationException extends RuntimeException {
    public InitializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public InitializationException(String message) {
        super(message);
    }
}
