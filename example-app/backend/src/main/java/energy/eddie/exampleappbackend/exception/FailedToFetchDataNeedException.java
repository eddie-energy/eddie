package energy.eddie.exampleappbackend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class FailedToFetchDataNeedException extends ResponseStatusException {
    public FailedToFetchDataNeedException() {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch data need from EDDIE instance!");
    }
}
