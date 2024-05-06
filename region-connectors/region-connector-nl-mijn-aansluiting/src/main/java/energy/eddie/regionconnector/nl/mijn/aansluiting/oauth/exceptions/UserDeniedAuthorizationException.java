package energy.eddie.regionconnector.nl.mijn.aansluiting.oauth.exceptions;

public class UserDeniedAuthorizationException extends Exception {
    public UserDeniedAuthorizationException() {
        super("User has denied authorization.");
    }
}
