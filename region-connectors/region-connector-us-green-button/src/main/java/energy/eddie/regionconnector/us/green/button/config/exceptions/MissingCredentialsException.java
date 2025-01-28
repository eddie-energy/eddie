package energy.eddie.regionconnector.us.green.button.config.exceptions;

public abstract class MissingCredentialsException extends Exception{
    protected MissingCredentialsException(String message, String company) {
        super(message.formatted(company));
    }
}
