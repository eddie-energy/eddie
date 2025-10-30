package energy.eddie.aiida.errors;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import energy.eddie.aiida.dtos.PatchOperation;
import energy.eddie.aiida.errors.installer.InstallerException;
import energy.eddie.aiida.errors.permission.DetailFetchingFailedException;
import energy.eddie.aiida.errors.permission.PermissionAlreadyExistsException;
import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.aiida.errors.permission.PermissionUnfulfillableException;
import energy.eddie.api.agnostic.EddieApiError;
import energy.eddie.api.agnostic.process.model.PermissionStateTransitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static energy.eddie.api.agnostic.GlobalConfig.ERRORS_PROPERTY_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler advice = new GlobalExceptionHandler();
    private final UUID permissionId = UUID.fromString("72831e2c-a01c-41b8-9db6-3f51670df7a5");

    @Test
    void givenHttpMessageNotReadableException_returnsBadRequest() {
        // Given
        var exception = new HttpMessageNotReadableException("", mock(HttpInputMessage.class));

        // When
        ResponseEntity<Map<String, List<EddieApiError>>> response = advice.handleHttpMessageNotReadableException(
                exception);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        var responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(1, responseBody.size());
        assertEquals(1, responseBody.get(ERRORS_PROPERTY_NAME).size());
        assertEquals("Invalid request body.", responseBody.get(ERRORS_PROPERTY_NAME).getFirst().message());
    }

    @Test
    void givenInvalidEnumValue_returnsErrorMessageAndPermittedValues() {
        // mock setup so it appears that the granularity field of TestClassWithGranularity caused the HttpMessageNotReadableException
        var mockJsonReference = mock(JsonMappingException.Reference.class);
        InvalidFormatException mockInvalidFormatEx = mock(InvalidFormatException.class);
        doReturn(PatchOperation.class).when(mockInvalidFormatEx).getTargetType();
        doReturn(List.of(mockJsonReference)).when(mockInvalidFormatEx).getPath();
        doReturn("operation").when(mockJsonReference).getFieldName();
        doReturn("FooBar").when(mockInvalidFormatEx).getValue();

        // Given
        var exception = new HttpMessageNotReadableException("", mockInvalidFormatEx, mock(HttpInputMessage.class));

        // When
        ResponseEntity<Map<String, List<EddieApiError>>> response = advice.handleHttpMessageNotReadableException(
                exception);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        var responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(1, responseBody.size());
        assertEquals(1, responseBody.get(ERRORS_PROPERTY_NAME).size());
        // Only the annotated values are included in the valid values array
        assertEquals("operation: Invalid enum value: 'FooBar'. Valid values: [REVOKE, ACCEPT, REJECT].",
                     responseBody.get(ERRORS_PROPERTY_NAME).getFirst().message());
    }

    @Test
    void givenMethodArgumentNotValidException_returnsBadRequest() {
        var expectedErrors = List.of(new EddieApiError("field1: Error message 1"),
                                     new EddieApiError("field2: Error message 2"));

        // Given
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getAllErrors()).thenReturn(createErrorFields());
        var exception = new MethodArgumentNotValidException(mock(MethodParameter.class), bindingResult);

        // When
        ResponseEntity<Map<String, List<EddieApiError>>> response = advice.handleMethodArgumentNotValidException(
                exception);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        var responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(1, responseBody.size());
        assertEquals(2, responseBody.get(ERRORS_PROPERTY_NAME).size());
        assertThat(responseBody.get(ERRORS_PROPERTY_NAME)).hasSameElementsAs(expectedErrors);
    }

    @Test
    void givenPermissionNotFoundException_returnsNotFound() {
        // Given
        var exception = new PermissionNotFoundException(permissionId);

        // When
        ResponseEntity<Map<String, List<EddieApiError>>> response = advice.handleNotFoundExceptions(exception);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        var responseBody = response.getBody();
        var message = "No permission with ID '%s' found.".formatted(permissionId);
        assertNotNull(responseBody);
        assertEquals(1, responseBody.size());
        assertEquals(1, responseBody.get(ERRORS_PROPERTY_NAME).size());
        assertEquals(message, responseBody.get(ERRORS_PROPERTY_NAME).getFirst().message());
    }

    @Test
    void givenPermissionAlreadyExistsException_returnsBadRequest() {
        // Given
        var exception = new PermissionAlreadyExistsException(permissionId);

        // When
        var response = advice.handleBadRequestExceptions(exception);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        var responseBody = response.getBody();
        var message = "Permission with ID '%s' already exists.".formatted(permissionId);
        assertNotNull(responseBody);
        assertEquals(1, responseBody.size());
        assertEquals(1, responseBody.get(ERRORS_PROPERTY_NAME).size());
        assertEquals(message, responseBody.get(ERRORS_PROPERTY_NAME).getFirst().message());
    }

    @Test
    void givenPermissionUnfulfillableException_returnsBadRequest() {
        // Given
        var exception = new PermissionUnfulfillableException("My Service");

        // When
        var response = advice.handleConflictExceptions(exception);

        // Then
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        var responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(1, responseBody.size());
        assertEquals(1, responseBody.get(ERRORS_PROPERTY_NAME).size());
        assertThat(responseBody.get(ERRORS_PROPERTY_NAME).getFirst().message()).startsWith(
                "Permission for service 'My Service' cannot be fulfilled");
    }

    @Test
    void givenPermissionStateTransitionException_returnsBadRequest() {
        // Given
        var exception = new PermissionStateTransitionException("fooBar", "desired", List.of("allowed"), "current");

        // When
        var response = advice.handleConflictExceptions(exception);

        // Then
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        var responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(1, responseBody.size());
        assertEquals(1, responseBody.get(ERRORS_PROPERTY_NAME).size());
        assertThat(responseBody.get(ERRORS_PROPERTY_NAME).getFirst().message()).isEqualTo(
                "Cannot transition permission 'fooBar' to state 'desired', as it is not in a one of the permitted states '[allowed]' but in state 'current'");
    }

    @Test
    void givenDetailFetchingFailedException_returnsServiceUnavailable() {
        // Given
        var exception = new DetailFetchingFailedException(permissionId);

        // When
        var response = advice.handleServiceUnavailableExceptions(exception);

        // Then
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        var responseBody = response.getBody();
        var message = "Failed to fetch permission details or MQTT credentials for permission '%s'".formatted(
                permissionId);
        assertNotNull(responseBody);
        assertEquals(1, responseBody.size());
        assertEquals(1, responseBody.get(ERRORS_PROPERTY_NAME).size());
        assertThat(responseBody.get(ERRORS_PROPERTY_NAME).getFirst().message()).isEqualTo(message);
    }

    @Test
    void givenDetailInstallerException_returnsSameStatus() {
        // Given
        var exception = new InstallerException(HttpStatus.BAD_GATEWAY, "Test exception");

        // When
        var response = advice.handleInstallerException(exception);

        // Then
        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        var responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(1, responseBody.size());
        assertEquals(1, responseBody.get(ERRORS_PROPERTY_NAME).size());
        assertThat(responseBody.get(ERRORS_PROPERTY_NAME).getFirst().message()).isEqualTo("Test exception");
    }

    private static List<ObjectError> createErrorFields() {
        List<ObjectError> errors = new ArrayList<>();
        errors.add(new FieldError("field1", "field1", "Error message 1"));
        errors.add(new FieldError("field2", "field2", "Error message 2"));
        return errors;
    }
}
