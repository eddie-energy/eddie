// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors;

import api.ValidationErrors;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import energy.eddie.aiida.errors.auth.InvalidUserException;
import energy.eddie.aiida.errors.auth.UnauthorizedException;
import energy.eddie.aiida.errors.datasource.DataSourceNotFoundException;
import energy.eddie.aiida.errors.datasource.DataSourceSecretGenerationNotSupportedException;
import energy.eddie.aiida.errors.datasource.InvalidDataSourceTypeException;
import energy.eddie.aiida.errors.datasource.modbus.ModbusConnectionException;
import energy.eddie.aiida.errors.datasource.modbus.ModbusDeviceConfigException;
import energy.eddie.aiida.errors.datasource.mqtt.MqttTlsCertificateNotFoundException;
import energy.eddie.aiida.errors.datasource.mqtt.MqttUnauthorizedException;
import energy.eddie.aiida.errors.datasource.mqtt.it.SinapsiAlflaEmptyConfigException;
import energy.eddie.aiida.errors.image.ImageFormatException;
import energy.eddie.aiida.errors.image.ImageNotFoundException;
import energy.eddie.aiida.errors.image.ImageReadException;
import energy.eddie.aiida.errors.permission.*;
import energy.eddie.aiida.errors.record.InboundRecordNotFoundException;
import energy.eddie.aiida.errors.record.LatestAiidaRecordNotFoundException;
import energy.eddie.api.agnostic.EddieApiError;
import energy.eddie.api.agnostic.process.model.PermissionStateTransitionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.InvalidTypeIdException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static energy.eddie.api.agnostic.GlobalConfig.ERRORS_PROPERTY_NAME;

@ControllerAdvice
public class GlobalExceptionHandler {
    /**
     * If the HttpMessageNotReadableException was caused by an invalid enum value, or an invalid type id,
     * a detailed error message including valid values is returned.
     * Otherwise, returns a generic error message.
     *
     * @param exception HttpMessageNotReadableException Exception that occurred during request parsing.
     * @return ResponseEntity with status code 400 and an error message.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, List<EddieApiError>>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception
    ) {
        var cause = exception.getMostSpecificCause();
        var errorDetails = switch (cause) {
            case InvalidTypeIdException invalidTypeIdEx -> {
                if (invalidTypeIdEx.getBaseType() == null) {
                    yield "Cannot infer a typing for the given request body.";
                }

                var baseTypeClass = invalidTypeIdEx.getBaseType().getRawClass();
                var typeInfo = baseTypeClass.getAnnotation(JsonTypeInfo.class);

                if (typeInfo == null || typeInfo.property() == null || typeInfo.property().isBlank()) {
                    yield "Cannot infer a property to assume a typing for the given request body.";
                }

                var subTypes = baseTypeClass.getAnnotation(JsonSubTypes.class);
                if (subTypes == null || subTypes.value() == null || subTypes.value().length == 0) {
                    yield "The %s property has no typing options. Please report this issue.".formatted(typeInfo.property());
                }

                var validValues = new String[subTypes.value().length];
                var subTypesValue = subTypes.value();
                for (int i = 0; i < subTypesValue.length; i++) {
                    var subType = subTypesValue[i];
                    var name = subType.name();
                    validValues[i] = name == null || name.isBlank() ? subType.value().getSimpleName() : name;
                }
                var validValuesString = Arrays.toString(validValues);

                if (invalidTypeIdEx.getTypeId() == null) {
                    yield "%s is missing or invalid. Valid values: %s.".formatted(typeInfo.property(),
                                                                                  validValuesString);
                }

                yield "Invalid %s: '%s'. Valid values: %s.".formatted(typeInfo.property(),
                                                                      invalidTypeIdEx.getTypeId(),
                                                                      validValuesString);
            }
            case InvalidFormatException invalidFormatEx when
                    invalidFormatEx.getTargetType() != null &&
                    invalidFormatEx.getTargetType().isEnum() -> {
                var propertyName = invalidFormatEx.getPath().getLast().getPropertyName();
                var validEnumConstants = invalidFormatEx.getTargetType().getEnumConstants();

                yield "%s: Invalid enum value: '%s'. Valid values: %s."
                        .formatted(propertyName,
                                   invalidFormatEx.getValue(),
                                   Arrays.toString(validEnumConstants));
            }
            default -> "Invalid request body.";
        };
        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(errorDetails)));
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, List<EddieApiError>>> handleMethodArgumentNotValidException
            (MethodArgumentNotValidException ex) {
        var errors = new ValidationErrors(ex).asErrorsList();
        return ResponseEntity.badRequest().body(Map.of(ERRORS_PROPERTY_NAME, errors));
    }

    @ExceptionHandler({
            ImageFormatException.class,
            ImageReadException.class,
            InvalidDataSourceTypeException.class,
            ModbusDeviceConfigException.class,
            PermissionAlreadyExistsException.class,
    })
    public ResponseEntity<Map<String, List<EddieApiError>>> handleBadRequestExceptions(Exception exception) {
        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(exception.getMessage())));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    @ExceptionHandler({
            InvalidUserException.class,
            MqttUnauthorizedException.class,
            UnauthorizedException.class,
    })
    public ResponseEntity<Map<String, List<EddieApiError>>> handleUnauthorizedException(Exception exception) {
        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(exception.getMessage())));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errors);
    }

    @ExceptionHandler({
            DataSourceSecretGenerationNotSupportedException.class
    })
    public ResponseEntity<Map<String, List<EddieApiError>>> handleForbiddenExceptions(Exception exception) {
        var message = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(exception.getMessage())));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(message);
    }

    @ExceptionHandler({
            DataSourceNotFoundException.class,
            ImageNotFoundException.class,
            InboundRecordNotFoundException.class,
            LatestAiidaRecordNotFoundException.class,
            LatestPermissionRecordNotFoundException.class,
            MqttTlsCertificateNotFoundException.class,
            PermissionNotFoundException.class,
            SinapsiAlflaEmptyConfigException.class
    })
    public ResponseEntity<Map<String, List<EddieApiError>>> handleNotFoundExceptions(Exception exception) {
        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(exception.getMessage())));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errors);
    }

    @ExceptionHandler({
            PermissionUnfulfillableException.class,
            PermissionStateTransitionException.class
    })
    public ResponseEntity<Map<String, List<EddieApiError>>> handleConflictExceptions(Exception exception) {
        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(exception.getMessage())));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errors);
    }

    @ExceptionHandler({
            DetailFetchingFailedException.class,
            ModbusConnectionException.class,
    })
    public ResponseEntity<Map<String, List<EddieApiError>>> handleServiceUnavailableExceptions(Exception exception) {
        var errors = Map.of(ERRORS_PROPERTY_NAME, List.of(new EddieApiError(exception.getMessage())));
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errors);
    }
}
