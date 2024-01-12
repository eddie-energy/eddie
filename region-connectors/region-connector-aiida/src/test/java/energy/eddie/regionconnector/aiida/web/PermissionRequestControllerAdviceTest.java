package energy.eddie.regionconnector.aiida.web;

import energy.eddie.api.agnostic.exceptions.DataNeedNotFoundException;
import energy.eddie.api.v0.process.model.PastStateException;
import energy.eddie.api.v0.process.model.PermissionRequestState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionRequestControllerAdviceTest {
    private final PermissionRequestControllerAdvice advice = new PermissionRequestControllerAdvice();
    @Mock
    private PermissionRequestState mockState;

    private static MethodArgumentNotValidException createMockExceptionWithErrorFields() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getAllErrors()).thenReturn(createErrorFields());
        return new MethodArgumentNotValidException(mock(MethodParameter.class), bindingResult);
    }

    private static List<ObjectError> createErrorFields() {
        List<ObjectError> errors = new ArrayList<>();
        errors.add(new FieldError("field1", "field1", "Error message 1"));
        errors.add(new FieldError("field2", "field2", "Error message 2"));
        return errors;
    }

    @Test
    void givenPastStateException_returnsInternalServerError() {
        ResponseEntity<Object> response = advice.handleStateTransitionException(new PastStateException(mockState));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertThat(response.toString()).contains("An error occurred while trying to transition a permission request to a new state");
    }

    @Test
    void givenMethodArgumentNotValidException_returnsBadRequestAndDescription() {
        // Given
        var ex = createMockExceptionWithErrorFields();

        // When
        ResponseEntity<Object> response = advice.handleMethodArgumentNotValid(ex, HttpHeaders.EMPTY,
                HttpStatus.I_AM_A_TEAPOT, mock(WebRequest.class));

        // Then
        assertNotNull(response);
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        String body = response.getBody().toString();
        assertThat(body)
                .contains("Error message 1")
                .contains("Error message 2");
    }

    @Test
    void givenHttpMessageNotReadableException_returnsBadRequest() {
        // Given

        // When
        ResponseEntity<Object> response = advice.handleHttpMessageNotReadable(
                mock(HttpMessageNotReadableException.class), HttpHeaders.EMPTY,
                HttpStatus.I_AM_A_TEAPOT, mock(WebRequest.class));

        // Then
        assertNotNull(response);
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        String body = response.getBody().toString();
        assertThat(body).contains("Failed to read request");
    }

    @Test
    void givenDataNeedNotFoundException_returnsBadRequest() {
        // Given
        String dataNeedId = "dataNeedId";
        DataNeedNotFoundException notFoundException = new DataNeedNotFoundException(dataNeedId);

        // When
        ResponseEntity<Object> response = advice.handleDataNeedNotFoundException(notFoundException);

        // Then
        assertNotNull(response);
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertThat(response.getBody().toString()).contains("No dataNeed with ID %s found".formatted(dataNeedId));
    }
}