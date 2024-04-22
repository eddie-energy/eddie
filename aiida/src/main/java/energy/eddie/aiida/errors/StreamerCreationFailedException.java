package energy.eddie.aiida.errors;

public class StreamerCreationFailedException extends Exception {
    public StreamerCreationFailedException(String message) {
        super(message);
    }

    public StreamerCreationFailedException(Throwable cause) {
        super(cause);
    }
}
