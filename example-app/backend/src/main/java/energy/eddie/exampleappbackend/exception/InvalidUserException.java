package energy.eddie.exampleappbackend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class InvalidUserException extends ResponseStatusException {
    public InvalidUserException() {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "Could not get UUID from current User!");
    }
}
