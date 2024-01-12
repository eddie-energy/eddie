package energy.eddie.core.dataneeds;

import energy.eddie.api.agnostic.exceptions.DataNeedNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class DataNeedsManagementControllerAdvice {
    @ExceptionHandler(DataNeedNotFoundException.class)
    public ResponseEntity<String> handleDataNeedNotFoundException(DataNeedNotFoundException exception) {
        return new ResponseEntity<>(exception.getMessage(), HttpStatus.NOT_FOUND);
    }
}