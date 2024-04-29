package energy.eddie.spring.regionconnector.extensions;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import energy.eddie.api.agnostic.EddieApiError;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.PastStateException;
import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.api.agnostic.process.model.PermissionStateTransitionException;
import energy.eddie.api.agnostic.process.model.SendToPermissionAdministratorException;
import energy.eddie.api.agnostic.process.model.states.CreatedPermissionRequestState;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.agnostic.process.model.validation.ValidationException;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.exceptions.JwtCreationFailedException;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import energy.eddie.regionconnector.shared.validation.SupportedGranularities;
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

import static energy.eddie.api.agnostic.GlobalConfig.ERRORS_PROPERTY_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class RegionConnectorsCommonControllerAdviceTest {
    private final RegionConnectorsCommonControllerAdvice advice = new RegionConnectorsCommonControllerAdvice();

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
        doReturn(Granularity.class).when(mockInvalidFormatEx).getTargetType();
        doReturn(List.of(mockJsonReference)).when(mockInvalidFormatEx).getPath();
        doReturn(TestClassWithGranularity.class).when(mockJsonReference).getFrom();
        doReturn("granularity").when(mockJsonReference).getFieldName();
        doReturn(Granularity.P1Y).when(mockInvalidFormatEx).getValue();

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
        assertEquals("granularity: Invalid enum value: 'P1Y'. Valid values: [PT15M, P1D].",
                     responseBody.get(ERRORS_PROPERTY_NAME).getFirst().message());
    }

    record TestClassWithGranularity(String ignored,
                                    @SupportedGranularities({Granularity.PT15M, Granularity.P1D})
                                    Granularity granularity) {
    }

    @Test
    void givenInvalidParseException_returnsErrorMessage() {
        InvalidFormatException mockInvalidFormatEx = mock(InvalidFormatException.class);
        when(mockInvalidFormatEx.getValue()).thenReturn("foo");
        when(mockInvalidFormatEx.getPath()).thenReturn(List.of(new JsonMappingException.Reference(null, "duration"),
                                                               new JsonMappingException.Reference(null, "start")));

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
        assertEquals("duration.start: Cannot parse value 'foo'.",
                     responseBody.get(ERRORS_PROPERTY_NAME).getFirst().message());
    }

    @Test
    void givenSendToPermissionAdministratorException_returnsBadRequestIfUserFault() {
        // Given
        var exception = new SendToPermissionAdministratorException(mock(PermissionRequestState.class),
                                                                   "Test message",
                                                                   true);

        // When
        ResponseEntity<Map<String, List<EddieApiError>>> response = advice.handleSendToPermissionAdministratorException(
                exception);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        var responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(1, responseBody.size());
        assertEquals(1, responseBody.get(ERRORS_PROPERTY_NAME).size());
        assertEquals("Test message", responseBody.get(ERRORS_PROPERTY_NAME).getFirst().message());
    }

    @Test
    void givenSendToPermissionAdministratorException_returnsInternalErrorIfNotUserFault() {
        // Given
        var exception = new SendToPermissionAdministratorException(mock(PermissionRequestState.class),
                                                                   "Test message",
                                                                   false);

        // When
        ResponseEntity<Map<String, List<EddieApiError>>> response = advice.handleSendToPermissionAdministratorException(
                exception);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        var responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(1, responseBody.size());
        assertEquals(1, responseBody.get(ERRORS_PROPERTY_NAME).size());
        assertEquals("Test message", responseBody.get(ERRORS_PROPERTY_NAME).getFirst().message());
    }

    @Test
    void givenStateTransitionException_returnsInternalServerError() {
        // Given
        var exception = new PastStateException(CreatedPermissionRequestState.class);

        // When
        ResponseEntity<Map<String, List<EddieApiError>>> response = advice.handleStateTransitionException(exception);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        var responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(1, responseBody.size());
        assertEquals(1, responseBody.get(ERRORS_PROPERTY_NAME).size());
        assertEquals("An error occurred while trying to transition a permission request to a new state.",
                     responseBody.get(ERRORS_PROPERTY_NAME).getFirst().message());
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

    private static List<ObjectError> createErrorFields() {
        List<ObjectError> errors = new ArrayList<>();
        errors.add(new FieldError("field1", "field1", "Error message 1"));
        errors.add(new FieldError("field2", "field2", "Error message 2"));
        return errors;
    }

    @Test
    void givenValidationException_returnsBadRequest() {
        var expectedErrors = List.of(
                new EddieApiError("field1: Error message 1"),
                new EddieApiError("field1: Error message 2"),
                new EddieApiError("field2: Error message 3"));

        // Given
        var exception = new ValidationException(mock(PermissionRequestState.class), createAttributeErrors());

        // When
        ResponseEntity<Map<String, List<EddieApiError>>> response = advice.handleStateValidationExceptions(exception);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        var responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(1, responseBody.size());
        assertEquals(3, responseBody.get(ERRORS_PROPERTY_NAME).size());
        assertThat(responseBody.get(ERRORS_PROPERTY_NAME)).hasSameElementsAs(expectedErrors);
    }

    private static List<AttributeError> createAttributeErrors() {
        return List.of(
                new AttributeError("field1", "Error message 1"),
                new AttributeError("field1", "Error message 2"),
                new AttributeError("field2", "Error message 3"));
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
        assertEquals("No permission with ID 'some-non-existing-id' found.",
                     responseBody.get(ERRORS_PROPERTY_NAME).getFirst().message());
    }

    @Test
    void givenJwtCreationFailedException_returnsInternalServerError() {
        // Given
        var exception = new JwtCreationFailedException(null);

        // When
        ResponseEntity<Map<String, List<EddieApiError>>> response = advice.handleJwtCreationFailedException(exception);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        var responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(1, responseBody.size());
        assertEquals(1, responseBody.get(ERRORS_PROPERTY_NAME).size());
        assertEquals("Failed to create JWT",
                     responseBody.get(ERRORS_PROPERTY_NAME).getFirst().message());
    }

    @Test
    void givenPermissionStateTransitionException_returnsBadRequest() {
        // Given
        var exception = new PermissionStateTransitionException("myId",
                                                               PermissionProcessStatus.ACCEPTED,
                                                               PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                                                               PermissionProcessStatus.FULFILLED);

        // When
        ResponseEntity<Map<String, List<EddieApiError>>> response = advice.handlePermissionStateTransitionException(
                exception);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        var responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(1, responseBody.size());
        assertEquals(1, responseBody.get(ERRORS_PROPERTY_NAME).size());
        assertEquals(
                "Cannot transition permission 'myId' to state 'ACCEPTED', as it is not in a one of the permitted states '[SENT_TO_PERMISSION_ADMINISTRATOR]' but in state 'FULFILLED'",
                responseBody.get(ERRORS_PROPERTY_NAME).getFirst().message());
    }
}
