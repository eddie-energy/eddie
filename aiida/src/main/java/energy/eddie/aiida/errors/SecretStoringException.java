package energy.eddie.aiida.errors;

import java.util.UUID;
import java.util.function.Supplier;

public class SecretStoringException extends Exception implements Supplier<SecretStoringException> {
    private static final String EXCEPTION_MESSAGE = "Failed to store secret";

    public SecretStoringException() {
        super(EXCEPTION_MESSAGE);
    }

    public SecretStoringException(UUID id, Throwable cause) {
        super(EXCEPTION_MESSAGE + " for " + id, cause);
    }

    @Override
    public SecretStoringException get() {
        return this;
    }
}
