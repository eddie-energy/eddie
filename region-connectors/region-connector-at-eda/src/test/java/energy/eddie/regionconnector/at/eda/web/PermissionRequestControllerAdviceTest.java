package energy.eddie.regionconnector.at.eda.web;

import energy.eddie.api.v0.process.model.PastStateException;
import energy.eddie.api.v0.process.model.PermissionRequestState;
import energy.eddie.api.v0.process.model.validation.ValidationException;
import energy.eddie.regionconnector.at.eda.permission.request.states.AtAcceptedPermissionRequestState;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PermissionRequestControllerAdviceTest {

    private static Stream<Arguments> validationErrors() {
        return Stream.of(
                Arguments.of(createMockExceptionWithErrorFields()),
                Arguments.of(createMockExceptionWithObjectErrors())
        );
    }

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

    private static MethodArgumentNotValidException createMockExceptionWithObjectErrors() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getAllErrors()).thenReturn(createObjectErrors());
        return new MethodArgumentNotValidException(mock(MethodParameter.class), bindingResult);
    }

    private static List<ObjectError> createObjectErrors() {
        List<ObjectError> errors = new ArrayList<>();
        errors.add(new ObjectError("field1", "Error message 1"));
        errors.add(new ObjectError("field2", "Error message 2"));
        return errors;
    }

    @Test
    void controllerAdviceStateTransitionException_returnsBadRequest() {
        // Given
        PermissionRequestControllerAdvice permissionRequestControllerAdvice = new PermissionRequestControllerAdvice();

        // When
        var res = permissionRequestControllerAdvice.stateTransitionException(new PastStateException(PermissionRequestState.class));

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
    }

    @Test
    void controllerAdviceHandleStateValidationException_returnsMapOfErrors() {
        // Given
        PermissionRequestControllerAdvice permissionRequestControllerAdvice = new PermissionRequestControllerAdvice();
        ValidationException exception = new ValidationException(new AtAcceptedPermissionRequestState(null), "field1", "Error message 1");

        // When
        Map<String, String> errors = permissionRequestControllerAdvice.handleStateValidationExceptions(exception);

        // Then
        assertAll(
                () -> assertEquals(1, errors.size()),
                () -> assertTrue(errors.containsKey("field1")),
                () -> assertEquals("Error message 1", errors.get("field1"))
        );
    }

    @ParameterizedTest
    @MethodSource("validationErrors")
    void handleValidationExceptionsReturnsMapOfErrors(MethodArgumentNotValidException exception) {
        // Given
        PermissionRequestControllerAdvice permissionRequestControllerAdvice = new PermissionRequestControllerAdvice();

        // When
        Map<String, String> errors = permissionRequestControllerAdvice.handleValidationExceptions(exception);

        // Then
        assertAll(
                () -> assertEquals(2, errors.size()),
                () -> assertTrue(errors.containsKey("field1")),
                () -> assertTrue(errors.containsKey("field2")),
                () -> assertEquals("Error message 1", errors.get("field1")),
                () -> assertEquals("Error message 2", errors.get("field2"))
        );
    }

    @Test
    void givenPermissionNotFoundException_returnsNotFound() {
        // Given
        PermissionRequestControllerAdvice advice = new PermissionRequestControllerAdvice();
        var exception = new PermissionNotFoundException("testId");

        // When
        var errors = advice.handlePermissionNotFoundException(exception);

        // Then
        assertAll(
                () -> assertEquals(1, errors.size()),
                () -> assertTrue(errors.containsKey("permissionId")),
                () -> assertEquals("No permission with ID testId found", errors.get("permissionId"))
        );
    }
}
