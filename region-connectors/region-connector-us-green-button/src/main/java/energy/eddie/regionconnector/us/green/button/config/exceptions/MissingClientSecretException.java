package energy.eddie.regionconnector.us.green.button.config.exceptions;

public class MissingClientSecretException extends Exception {
    private static final String MESSAGE = "No client secret found for the given utility. Please check the configuration.";

    public MissingClientSecretException() {
        super(MESSAGE);
    }
}
