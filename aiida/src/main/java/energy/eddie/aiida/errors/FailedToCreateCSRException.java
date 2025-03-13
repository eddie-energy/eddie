package energy.eddie.aiida.errors;

public class FailedToCreateCSRException extends RuntimeException {
    public FailedToCreateCSRException() {
        super("Failed to create Certificate Signing Request");
    }
}
