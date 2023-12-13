package energy.eddie.regionconnector.es.datadis.web;

import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.regionconnector.shared.dtos.ErrorResponse;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;

@ControllerAdvice
public class PermissionControllerAdvice {
    private static final Logger LOGGER_ADVICE = LoggerFactory.getLogger(PermissionControllerAdvice.class);

    private static ResponseEntity<Object> createErrorResponse(List<String> errors, HttpStatus status) {
        return new ResponseEntity<>(new ErrorResponse(errors), status);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex
    ) {
        List<String> errors = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .toList();

        return createErrorResponse(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(PermissionNotFoundException.class)
    public ResponseEntity<Object> handlePermissionNotFoundException(PermissionNotFoundException exception) {
        var errors = List.of(exception.getMessage());

        return createErrorResponse(errors, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(value = {StateTransitionException.class})
    protected ResponseEntity<Object> handleStateTransitionException(StateTransitionException stateTransitionException) {
        LOGGER_ADVICE.info("Error occurred while trying to transition a state", stateTransitionException);

        var errors = List.of("An error occurred while trying to transition a permission request to a new state");

        return createErrorResponse(errors, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
