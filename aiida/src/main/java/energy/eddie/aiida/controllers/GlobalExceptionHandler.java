package energy.eddie.aiida.controllers;

import api.ValidationErrors;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import energy.eddie.aiida.errors.*;
import energy.eddie.api.agnostic.EddieApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    public static final String ERRORS_PROPERTY_NAME = "errors";
    public static final String ERRORS_JSON_PATH = "$." + ERRORS_PROPERTY_NAME;

    /**
     * If the HttpMessageNotReadableException was caused by an invalid enum value, a detailed error message including
     * valid enum values is returned, otherwise a generic error message is returned.
     *
     * @param exception HttpMessageNotReadableException Exception that occurred during request parsing.
     * @return ResponseEntity with status code 400 and an error message.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, List<EddieApiError>>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception
    ) {
        String errorDetails = "Invalid request body.";

        if (isEnumCauseOfException(exception)) {
            var invalidFormatEx = (InvalidFormatException) exception.getCause();

            if (invalidFormatEx != null) {
                var fieldName = invalidFormatEx.getPath().getLast().getFieldName();
                Object[] validEnumConstants = invalidFormatEx.getTargetType().getEnumConstants();

                errorDetails = String.format("%s: Invalid enum value: '%s'. Valid values: %s.",
                                             fieldName,
                                             invalidFormatEx.getValue(),
                                             Arrays.toString(validEnumConstants));
            }
        }
        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(errorDetails)));
        return ResponseEntity.badRequest().body(errors);
    }

    /**
     * @param exception HttpMessageNotReadableException that might have been caused by an Enum not being able to be
     *                  matched.
     * @return True if the passed exception is an {@link InvalidFormatException} and the target that couldn't be matched
     * is an Enum.
     */
    private boolean isEnumCauseOfException(HttpMessageNotReadableException exception) {
        return exception.getCause() instanceof InvalidFormatException invalidFormatEx
                && (invalidFormatEx.getTargetType() != null && invalidFormatEx.getTargetType().isEnum());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, List<EddieApiError>>> handleMethodArgumentNotValidException
            (MethodArgumentNotValidException ex) {
        var errors = new ValidationErrors(ex).asErrorsList();
        return ResponseEntity.badRequest().body(Map.of(ERRORS_PROPERTY_NAME, errors));
    }

    @ExceptionHandler(PermissionNotFoundException.class)
    public ResponseEntity<Map<String, List<EddieApiError>>> handlePermissionNotFoundException
            (PermissionNotFoundException exception) {
        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(exception.getMessage())));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errors);
    }

    @ExceptionHandler(value = {InvalidPermissionRevocationException.class})
    protected ResponseEntity<Map<String, List<EddieApiError>>> handleInvalidPermissionRevocationException(
            InvalidPermissionRevocationException exception
    ) {
        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(exception.getMessage())));
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(value = {InvalidPatchOperationException.class})
    protected ResponseEntity<Map<String, List<EddieApiError>>> handleInvalidPatchOperationException(
            InvalidPatchOperationException exception
    ) {
        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(exception.getMessage())));
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(value = {PermissionStartFailedException.class})
    protected ResponseEntity<Object> handlePermissionStartFailedException(PermissionStartFailedException ignored) {
        var errors = Map.of(ERRORS_PROPERTY_NAME,
                            List.of(new EddieApiError("Failed to start permission, please try again later.")));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errors);
    }

    @ExceptionHandler(value = {PermissionAlreadyExistsException.class})
    protected ResponseEntity<Map<String, List<EddieApiError>>> handlePermissionAlreadyExistsException(
            PermissionAlreadyExistsException exception
    ) {
        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(exception.getMessage())));
        return ResponseEntity.badRequest().body(errors);
    }
}
