package energy.eddie.spring.regionconnector.extensions;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import energy.eddie.api.agnostic.EddieApiError;
import energy.eddie.api.agnostic.RegionConnectorExtension;
import energy.eddie.api.agnostic.exceptions.DataNeedNotFoundException;
import energy.eddie.api.agnostic.process.model.SendToPermissionAdministratorException;
import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.api.agnostic.process.model.validation.ValidationException;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import energy.eddie.regionconnector.shared.validation.SupportedGranularities;
import energy.eddie.regionconnector.shared.web.StateValidationErrors;
import energy.eddie.regionconnector.shared.web.ValidationErrors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.lang.reflect.Field;
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
    public ResponseEntity<Map<String, List<EddieApiError>>> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception) {
        LOGGER.debug("Invalid request body", exception);
        String errorDetails = "Invalid request body.";

        if (isEnumCauseOfException(exception)) {
            var invalidFormatEx = (InvalidFormatException) exception.getCause();

            var fieldName = invalidFormatEx.getPath().getLast().getFieldName();
            Object[] validEnumConstants = getValidEnumConstantsAndFieldName(invalidFormatEx);

            errorDetails = String.format("%s: Invalid enum value: '%s'. Valid values: %s.",
                                         fieldName, invalidFormatEx.getValue(), Arrays.toString(validEnumConstants));
        } else if (isInvalidFormatExceptionCauseOfException(exception)) {
            InvalidFormatException formatException = ((InvalidFormatException) exception.getCause());
            String completeFieldName = formatException
                    .getPath()
                    .stream()
                    .map(JsonMappingException.Reference::getFieldName)
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
     * Returns an array of the valid enum constants for the field that could not be matched.
     * If the field is of type {@link energy.eddie.api.agnostic.Granularity} and annotated with
     * {@link SupportedGranularities}, only the supported granularities are returned.
     *
     * @param invalidFormatEx Exception that was caused by an invalid enum value.
     * @return Array of valid enum constants.
     */
    private Object[] getValidEnumConstantsAndFieldName(InvalidFormatException invalidFormatEx) {
        try {
            // try to get array of supported granularities from annotation
            var reference = invalidFormatEx.getPath().getLast();
            var fieldName = reference.getFieldName();
            var fieldClass = reference.getFrom();
            Class<?> fromClass = Class.forName(((Class<?>) fieldClass).getName());
            Field field = fromClass.getDeclaredField(fieldName);
            SupportedGranularities annotation = field.getAnnotation(SupportedGranularities.class);
            return annotation.value();
        } catch (Exception ignoredException) {
            // the field is either not of type Granularity or not annotated, treat it as a normal enum field and return all enum constants
        }

        return invalidFormatEx.getTargetType().getEnumConstants();
    }

    /**
     * @param exception HttpMessageNotReadableException that might have been caused by an Enum not being able to be matched.
     * @return True if the passed exception is an {@link InvalidFormatException} and the target that couldn't be matched is an Enum.
     */
    private boolean isEnumCauseOfException(HttpMessageNotReadableException exception) {
        return exception.getCause() instanceof InvalidFormatException invalidFormatEx
                && (invalidFormatEx.getTargetType() != null && invalidFormatEx.getTargetType().isEnum());
    }

    @ExceptionHandler(value = {SendToPermissionAdministratorException.class})
    protected ResponseEntity<Map<String, List<EddieApiError>>> handleSendToPermissionAdministratorException(SendToPermissionAdministratorException exception) {
        LOGGER.info("Error occurred while sending a permission request to a PA", exception);

        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(exception.getMessage())));
        var status = exception.userFault() ? HttpStatus.BAD_REQUEST : HttpStatus.INTERNAL_SERVER_ERROR;

        return ResponseEntity.status(status).body(errors);
    }

    @ExceptionHandler(StateTransitionException.class)
    // Use ResponseEntity instead of @ResponseStatus to be able to test the return status code and as it's recommended for REST APIs
    public ResponseEntity<Map<String, List<EddieApiError>>> handleStateTransitionException(StateTransitionException
                                                                                                   stateTransitionException) {
        var errorMsg = "An error occurred while trying to transition a permission request to a new state.";
        LOGGER.warn(errorMsg, stateTransitionException);

        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(errorMsg)));
        return ResponseEntity.internalServerError().body(errors);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, List<EddieApiError>>> handleMethodArgumentNotValidException
            (MethodArgumentNotValidException ex) {
        var errors = new ValidationErrors(ex).asErrorsList();
        return ResponseEntity.badRequest().body(Map.of(ERRORS_PROPERTY_NAME, errors));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, List<EddieApiError>>> handleStateValidationExceptions(ValidationException ex) {
        var errors = new StateValidationErrors(ex).asErrorsList();
        return ResponseEntity.badRequest().body(Map.of(ERRORS_PROPERTY_NAME, errors));
    }

    @ExceptionHandler(PermissionNotFoundException.class)
    public ResponseEntity<Map<String, List<EddieApiError>>> handlePermissionNotFoundException
            (PermissionNotFoundException exception) {
        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(exception.getMessage())));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errors);
    }

    @ExceptionHandler(DataNeedNotFoundException.class)
    public ResponseEntity<Map<String, List<EddieApiError>>> handleDataNeedNotFoundException
            (DataNeedNotFoundException exception) {
        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(exception.getMessage())));

        if (exception.isBadRequest())
            return ResponseEntity.badRequest().body(errors);
        else
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errors);
    }
}
