package energy.eddie.aiida.controllers;

import energy.eddie.aiida.dtos.ErrorResponse;
import energy.eddie.aiida.errors.ConnectionStatusMessageSendFailedException;
import energy.eddie.aiida.errors.InvalidPatchOperationException;
import energy.eddie.aiida.errors.InvalidPermissionRevocationException;
import energy.eddie.aiida.errors.PermissionNotFoundException;
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
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
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

    @ExceptionHandler(value = {PermissionNotFoundException.class})
    protected ResponseEntity<Object> handlePermissionNotFoundException(PermissionNotFoundException ex) {
        var errors = List.of(ex.getMessage());

        return createErrorResponse(errors, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(value = {InvalidPermissionRevocationException.class})
    protected ResponseEntity<Object> handleInvalidPermissionRevocationException(InvalidPermissionRevocationException ex) {
        var errors = List.of(ex.getMessage());

        return createErrorResponse(errors, HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(value = {InvalidPatchOperationException.class})
    protected ResponseEntity<Object> handleInvalidPatchOperationException(InvalidPatchOperationException ex) {
        var errors = List.of(ex.getMessage());

        return createErrorResponse(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = {ConnectionStatusMessageSendFailedException.class})
    protected ResponseEntity<Object> handleConnectionStatusMessageSendFailedException(ConnectionStatusMessageSendFailedException ignored) {
        var errors = List.of("Failed to setup permission, please try again later.");

        return createErrorResponse(errors, HttpStatus.INTERNAL_SERVER_ERROR);
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
}