package energy.eddie.regionconnector.us.green.button.config.exceptions;

public class MissingClientIdException extends Exception {
    private static final String MESSAGE = "No client id found for the given utility. Please check the configuration.";

    public MissingClientIdException() {
        super(MESSAGE);
    }
}
