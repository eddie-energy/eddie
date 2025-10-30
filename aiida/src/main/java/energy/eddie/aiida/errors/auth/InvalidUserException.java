package energy.eddie.aiida.errors.auth;

public class InvalidUserException extends Exception {
    public InvalidUserException() {
        super("Could not get UUID from current User!");
    }
}
