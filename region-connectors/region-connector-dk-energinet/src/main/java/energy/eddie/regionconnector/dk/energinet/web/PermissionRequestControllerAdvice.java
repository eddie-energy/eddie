package energy.eddie.regionconnector.dk.energinet.web;

import energy.eddie.api.v0.process.model.SendToPermissionAdministratorException;
import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.api.v0.process.model.validation.ValidationException;
import energy.eddie.regionconnector.shared.dtos.ErrorResponse;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

@ControllerAdvice
public class PermissionRequestControllerAdvice extends ResponseEntityExceptionHandler {
    private static final Logger LOGGER_ADVICE = LoggerFactory.getLogger(PermissionRequestControllerAdvice.class);

    private static ResponseEntity<Object> createErrorResponse(List<String> errors, HttpStatus status) {
        return new ResponseEntity<>(new ErrorResponse(errors), status);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request
    ) {
        List<String> errors = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .toList();

        return createErrorResponse(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = {StateTransitionException.class})
    protected ResponseEntity<Object> handleStateTransitionException(StateTransitionException stateTransitionException) {
        LOGGER_ADVICE.info("Error occurred while trying to transition a state", stateTransitionException);

        var errors = List.of("An error occurred while trying to transition a permission request to a new state");

        return createErrorResponse(errors, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Object> handleValidationException(ValidationException exception) {
        List<String> errors = exception.errors().stream()
                .map(error -> "%s: %s".formatted(error.name(), error.message()))
                .toList();

        return createErrorResponse(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = {SendToPermissionAdministratorException.class})
    protected ResponseEntity<Object> handleSendToPermissionAdministratorException(SendToPermissionAdministratorException exception) {
        LOGGER_ADVICE.info("Error occurred while sending a permission request to a PA", exception);

        var status = exception.userFault() ? HttpStatus.BAD_REQUEST : HttpStatus.INTERNAL_SERVER_ERROR;

        return createErrorResponse(List.of(exception.getMessage()), status);
    }

    @ExceptionHandler(PermissionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<Object> handlePermissionNotFoundException(PermissionNotFoundException exception) {
        return createErrorResponse(List.of(exception.getMessage()), HttpStatus.NOT_FOUND);
    }
}
