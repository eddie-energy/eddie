// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.api.agnostic.EddieApiError;
import energy.eddie.api.agnostic.RegionConnectorExtension;
import energy.eddie.api.agnostic.process.model.PermissionStateTransitionException;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.regionconnector.shared.exceptions.JwtCreationFailedException;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import energy.eddie.regionconnector.shared.web.ValidationErrors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static energy.eddie.api.agnostic.GlobalConfig.ERRORS_PROPERTY_NAME;

@RegionConnectorExtension
@RestControllerAdvice
public class RegionConnectorsCommonControllerAdvice {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegionConnectorsCommonControllerAdvice.class);

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
        LOGGER.debug("Invalid request body", exception);
        String errorDetails = "Invalid request body.";

        if (isEnumCauseOfException(exception)) {
            var invalidFormatEx = (InvalidFormatException) exception.getCause();

            var fieldName = invalidFormatEx.getPath().getLast().getPropertyName();
            Object[] validEnumConstants = invalidFormatEx.getTargetType().getEnumConstants();

            errorDetails = String.format("%s: Invalid enum value: '%s'. Valid values: %s.",
                                         fieldName, invalidFormatEx.getValue(), Arrays.toString(validEnumConstants));
        } else if (isInvalidFormatExceptionCauseOfException(exception)) {
            InvalidFormatException formatException = ((InvalidFormatException) exception.getCause());
            String completeFieldName = formatException
                    .getPath()
                    .stream()
                    .map(JacksonException.Reference::getPropertyName)
                    .collect(Collectors.joining("."));

            errorDetails = String.format("%s: Cannot parse value '%s'.", completeFieldName, formatException.getValue());
        }
        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(errorDetails)));
        return ResponseEntity.badRequest().body(errors);
    }

    /**
     * @param exception {@link HttpMessageNotReadableException} to check.
     * @return True if the {@code exception} was caused by an {@link InvalidFormatException}.
     */
    private boolean isInvalidFormatExceptionCauseOfException(HttpMessageNotReadableException exception) {
        return exception.getCause() instanceof InvalidFormatException invalidFormatEx
                && invalidFormatEx.getPath() != null;
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

    @ExceptionHandler(JwtCreationFailedException.class)
    public ResponseEntity<Map<String, List<EddieApiError>>> handleJwtCreationFailedException
            (JwtCreationFailedException exception) {
        LOGGER.error("JwtCreationFailedException exception occurred", exception);
        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(exception.getMessage())));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errors);
    }

    @ExceptionHandler(PermissionStateTransitionException.class)
    public ResponseEntity<Map<String, List<EddieApiError>>> handlePermissionStateTransitionException(
            PermissionStateTransitionException ex
    ) {
        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(ex.getMessage())));
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(DataNeedNotFoundException.class)
    public ResponseEntity<Map<String, List<EddieApiError>>> handleDataNeedNotFoundException(
            DataNeedNotFoundException ex
    ) {
        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(ex.getMessage())));
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(UnsupportedDataNeedException.class)
    public ResponseEntity<Map<String, List<EddieApiError>>> handleUnsupportedDataNeedException(
            UnsupportedDataNeedException ex
    ) {
        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(ex.getMessage())));
        return ResponseEntity.badRequest().body(errors);
    }
}
