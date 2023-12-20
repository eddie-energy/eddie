package energy.eddie.regionconnector.at.eda.web;

import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.api.v0.process.model.validation.ValidationException;
import energy.eddie.regionconnector.shared.web.StateValidationErrors;
import energy.eddie.regionconnector.shared.web.ValidationErrors;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class PermissionRequestControllerAdvice {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestControllerAdvice.class);

    @ExceptionHandler(value = {StateTransitionException.class})
    public ResponseEntity<String> stateTransitionException(StateTransitionException stateTransitionException) {
        LOGGER.info("Error occurred while trying to transition a state", stateTransitionException);
        return new ResponseEntity<>(stateTransitionException.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        return new ValidationErrors(ex).asMap();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ValidationException.class)
    public Map<String, String> handleStateValidationExceptions(ValidationException ex) {
        return new StateValidationErrors(ex).asMap();
    }

    @ExceptionHandler(PermissionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handlePermissionNotFoundException(PermissionNotFoundException exception) {
        return Map.of("permissionId", exception.getMessage());
    }
}
