package energy.eddie.regionconnector.at.eda.web;

import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.api.v0.process.model.validation.AttributeError;
import energy.eddie.api.v0.process.model.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

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
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError fieldError) {
                String fieldName = fieldError.getField();
                String errorMessage = fieldError.getDefaultMessage();
                errors.put(fieldName, errorMessage);
            } else {
                String objectName = error.getObjectName();
                String errorMessage = error.getDefaultMessage();
                errors.put(objectName, errorMessage);
            }
        });
        return errors;
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ValidationException.class)
    public Map<String, String> handleStateValidationExceptions(ValidationException ex) {
        return ex.errors()
                .stream()
                .collect(Collectors.toMap(AttributeError::name, AttributeError::message));
    }
}
