package energy.eddie.aiida.controllers;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import energy.eddie.aiida.dtos.PatchOperation;
import energy.eddie.aiida.dtos.PatchPermissionDto;
import energy.eddie.aiida.errors.InvalidPatchOperationException;
import energy.eddie.aiida.errors.PermissionAlreadyExistsException;
import energy.eddie.aiida.errors.PermissionNotFoundException;
import energy.eddie.api.agnostic.EddieApiError;
import org.junit.jupiter.api.Test;
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

import static energy.eddie.aiida.controllers.GlobalExceptionHandler.ERRORS_PROPERTY_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler advice = new GlobalExceptionHandler();

    private static List<ObjectError> createErrorFields() {
        List<ObjectError> errors = new ArrayList<>();
        errors.add(new FieldError("field1", "field1", "Error message 1"));
        errors.add(new FieldError("field2", "field2", "Error message 2"));
        return errors;
    }

    @Test
    void givenHttpMessageNotReadableException_returnsBadRequest() {
        // Given
        var exception = new HttpMessageNotReadableException("", mock(HttpInputMessage.class));

        // When
        ResponseEntity<Map<String, List<EddieApiError>>> response = advice.handleHttpMessageNotReadableException(exception);

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
        doReturn(PatchPermissionDto.class).when(mockJsonReference).getFrom();
        doReturn("operation").when(mockJsonReference).getFieldName();
        doReturn("FooBar").when(mockInvalidFormatEx).getValue();

        // Given
        var exception = new HttpMessageNotReadableException("", mockInvalidFormatEx, mock(HttpInputMessage.class));

        // When
        ResponseEntity<Map<String, List<EddieApiError>>> response = advice.handleHttpMessageNotReadableException(exception);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        var responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(1, responseBody.size());
        assertEquals(1, responseBody.get(ERRORS_PROPERTY_NAME).size());
        // Only the annotated values are included in the valid values array
        assertEquals("operation: Invalid enum value: 'FooBar'. Valid values: [REVOKE_PERMISSION].", responseBody.get(ERRORS_PROPERTY_NAME).getFirst().message());
    }

    @Test
    void givenMethodArgumentNotValidException_returnsBadRequest() {
        var expectedErrors = List.of(
                new EddieApiError("field1: Error message 1"),
                new EddieApiError("field2: Error message 2"));

        // Given
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getAllErrors()).thenReturn(createErrorFields());
        var exception = new MethodArgumentNotValidException(mock(MethodParameter.class), bindingResult);

        // When
        ResponseEntity<Map<String, List<EddieApiError>>> response = advice.handleMethodArgumentNotValidException(exception);

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
        var exception = new PermissionNotFoundException("some-non-existing-id");

        // When
        ResponseEntity<Map<String, List<EddieApiError>>> response = advice.handlePermissionNotFoundException(exception);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        var responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(1, responseBody.size());
        assertEquals(1, responseBody.get(ERRORS_PROPERTY_NAME).size());
        assertEquals("No permission with ID 'some-non-existing-id' found.", responseBody.get(ERRORS_PROPERTY_NAME).getFirst().message());
    }

    @Test
    void givenInvalidPatchOperation_returnsBadRequest() {
        // Given
        var exception = new InvalidPatchOperationException();

        // When
        var response = advice.handleInvalidPatchOperationException(exception);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        var responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(1, responseBody.size());
        assertEquals(1, responseBody.get(ERRORS_PROPERTY_NAME).size());
        assertEquals("Invalid PatchOperation, permitted values are: [REVOKE_PERMISSION].", responseBody.get(ERRORS_PROPERTY_NAME).getFirst().message());
    }

    @Test
    void givenPermissionAlreadyExistsException_returnsBadRequest() {
        // Given
        var exception = new PermissionAlreadyExistsException("testId");

        // When
        var response = advice.handlePermissionAlreadyExistsException(exception);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        var responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(1, responseBody.size());
        assertEquals(1, responseBody.get(ERRORS_PROPERTY_NAME).size());
        assertEquals("Permission with ID 'testId' already exists.",
                     responseBody.get(ERRORS_PROPERTY_NAME).getFirst().message());
    }
}
