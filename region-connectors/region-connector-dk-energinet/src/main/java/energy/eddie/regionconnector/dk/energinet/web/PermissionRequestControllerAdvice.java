package energy.eddie.regionconnector.dk.energinet.web;

import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.regionconnector.dk.energinet.dtos.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
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

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            @NonNull HttpMessageNotReadableException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request
    ) {
        var errors = List.of("Failed to read request");

        return createErrorResponse(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = {StateTransitionException.class})
    protected ResponseEntity<Object> handleStateTransitionException(StateTransitionException stateTransitionException) {
        LOGGER_ADVICE.info("Error occurred while trying to transition a state", stateTransitionException);

        var errors = List.of("Error occurred while trying to transition a state");

        return createErrorResponse(errors, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
