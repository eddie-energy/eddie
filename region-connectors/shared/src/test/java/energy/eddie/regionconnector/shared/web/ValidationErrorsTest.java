package energy.eddie.regionconnector.shared.web;

import energy.eddie.api.agnostic.EddieApiError;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ValidationErrorsTest {
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

    @ParameterizedTest
    @MethodSource("validationErrors")
    void givenMethodArgumentNotValidException_returnsMapOfList(MethodArgumentNotValidException exception) {
        // Given
        ValidationErrors validationErrors = new ValidationErrors(exception);

        // When
        List<EddieApiError> errors = validationErrors.asErrorsList();

        // Then
        assertAll(
                () -> assertEquals(2, errors.size()),
                () -> assertThat(errors).hasSameElementsAs(List.of(
                        new EddieApiError("field1: Error message 1"),
                        new EddieApiError("field2: Error message 2")))
        );
    }
}