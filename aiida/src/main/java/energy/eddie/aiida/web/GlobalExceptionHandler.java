package energy.eddie.aiida.web;

import api.ValidationErrors;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import energy.eddie.aiida.errors.*;
import energy.eddie.aiida.errors.ModbusConnectionException;
import energy.eddie.aiida.errors.image.ImageFormatException;
import energy.eddie.aiida.errors.image.ImageNotFoundException;
import energy.eddie.aiida.errors.image.ImageReadException;
import energy.eddie.api.agnostic.EddieApiError;
import energy.eddie.api.agnostic.process.model.PermissionStateTransitionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static energy.eddie.api.agnostic.GlobalConfig.ERRORS_PROPERTY_NAME;

@ControllerAdvice
public class GlobalExceptionHandler {

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

    @ExceptionHandler(value = {InvalidPatchOperationException.class})
    protected ResponseEntity<Map<String, List<EddieApiError>>> handleInvalidPatchOperationException(
            InvalidPatchOperationException exception
    ) {
        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(exception.getMessage())));
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(value = {PermissionAlreadyExistsException.class})
    protected ResponseEntity<Map<String, List<EddieApiError>>> handlePermissionAlreadyExistsException(
            PermissionAlreadyExistsException exception
    ) {
        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(exception.getMessage())));
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(value = {PermissionUnfulfillableException.class})
    protected ResponseEntity<Map<String, List<EddieApiError>>> handlePermissionUnfulfillableException(
            PermissionUnfulfillableException exception
    ) {
        // TODO GH-1040 proper error message
        var message = "Permission for service '%s' cannot be fulfilled by your AIIDA, because...".formatted(exception.serviceName());
        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(message)));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errors);
    }

    @ExceptionHandler(value = {PermissionStateTransitionException.class})
    protected ResponseEntity<Map<String, List<EddieApiError>>> handlePermissionStateTransitionException(
            PermissionStateTransitionException exception
    ) {
        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(exception.getMessage())));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errors);
    }

    @ExceptionHandler(value = {DetailFetchingFailedException.class})
    protected ResponseEntity<Map<String, List<EddieApiError>>> handleDetailFetchingFailedException(
            DetailFetchingFailedException exception
    ) {
        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(exception.getMessage())));
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errors);
    }

    @ExceptionHandler(value = {InstallerException.class})
    protected ResponseEntity<Map<String, List<EddieApiError>>> handleInstallerException(InstallerException exception) {
        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(exception.getMessage())));
        return ResponseEntity.status(exception.httpStatus()).body(errors);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, List<EddieApiError>>> handleUnauthorizedException(UnauthorizedException exception) {
        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(exception.getMessage())));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errors);
    }

    @ExceptionHandler(value = {InvalidDataSourceTypeException.class})
    protected ResponseEntity<Map<String, List<EddieApiError>>> handleInvalidDataSourceTypeException(
            InvalidDataSourceTypeException exception
    ) {
        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(exception.getMessage())));
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(DataSourceNotFoundException.class)
    public ResponseEntity<Map<String, List<EddieApiError>>> handleDataServiceNotFoundException(DataSourceNotFoundException exception) {
        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(exception.getMessage())));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errors);
    }

    @ExceptionHandler(ImageNotFoundException.class)
    public ResponseEntity<Map<String, List<EddieApiError>>> handleImageNotFoundException(ImageNotFoundException exception) {
        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(exception.getMessage())));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errors);
    }

    @ExceptionHandler(ImageReadException.class)
    public ResponseEntity<Map<String, List<EddieApiError>>> handleImageReadException(ImageReadException exception) {
        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(exception.getMessage())));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    @ExceptionHandler(ImageFormatException.class)
    public ResponseEntity<Map<String, List<EddieApiError>>> handleImageFormatException(ImageFormatException exception) {
        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(exception.getMessage())));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    @ExceptionHandler(value = {InboundRecordNotFoundException.class})
    protected ResponseEntity<Map<String, List<EddieApiError>>> handleInboundRecordNotFoundException(
            InboundRecordNotFoundException exception
    ) {
        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(exception.getMessage())));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errors);
    }

    @ExceptionHandler(value = {MqttUnauthorizedException.class})
    protected ResponseEntity<Map<String, List<EddieApiError>>> handleMqttUnauthorizedException(MqttUnauthorizedException exception) {
        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(exception.getMessage())));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errors);
    }

    @ExceptionHandler(value = {MqttTlsCertificateNotFoundException.class})
    protected ResponseEntity<Map<String, List<EddieApiError>>> handleMqttCertificateNotFoundException(
            MqttTlsCertificateNotFoundException exception) {
        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(exception.getMessage())));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errors);
    }

    @ExceptionHandler(value = {ModbusConnectionException.class})
    protected ResponseEntity<Map<String, List<EddieApiError>>> handleModbusConnectionException(ModbusConnectionException exception) {
        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(exception.getMessage())));
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errors);
    }

    @ExceptionHandler(LatestAiidaRecordNotFoundException.class)
    public ResponseEntity<Map<String, List<EddieApiError>>> handleLatestAiidaRecordNotFound(LatestAiidaRecordNotFoundException exception) {
        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(exception.getMessage())));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errors);
    }

    @ExceptionHandler(LatestPermissionRecordNotFoundException.class)
    public ResponseEntity<Map<String, List<EddieApiError>>> handleLatestPermissionRecordNotFound(LatestPermissionRecordNotFoundException exception) {
        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(exception.getMessage())));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errors);
    }

    @ExceptionHandler(value = {SinapsiAlflaEmptyConfigException.class})
    protected ResponseEntity<Map<String, List<EddieApiError>>> handleSinapsiAlflaEmptyConfigException(SinapsiAlflaEmptyConfigException exception) {
        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(exception.getMessage())));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errors);
    }
}
