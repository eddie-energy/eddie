package energy.eddie.aiida.errors;

import java.util.function.Supplier;

public class SecretDeletionException extends Exception implements Supplier<SecretDeletionException> {
    private static final String EXCEPTION_MESSAGE = "Failed to delete secret";

    public SecretDeletionException() {
        super(EXCEPTION_MESSAGE);
    }

    public SecretDeletionException(String alias, Throwable cause) {
        super(EXCEPTION_MESSAGE + " for " + alias, cause);
    }

    @Override
    public SecretDeletionException get() {
        return this;
    }
}
