package energy.eddie.regionconnector.shared.web;

import energy.eddie.api.agnostic.EddieApiError;
import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.agnostic.process.model.validation.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class StateValidationErrorsTest {
    @Test
    void asMap_returnsMapOfErrors() {
        var expectedErrors = List.of(
                new EddieApiError("field1: Error message 1"),
                new EddieApiError("field1: Error message 2"),
                new EddieApiError("field2: Error message 3"));
        // Given
        ValidationException exception = new ValidationException(mock(PermissionRequestState.class), createAttributeErrors());
        StateValidationErrors stateValidationErrors = new StateValidationErrors(exception);

        // When
        List<EddieApiError> errors = stateValidationErrors.asErrorsList();

        // Then
        assertAll(
                () -> assertEquals(3, errors.size()),
                () -> assertThat(errors).hasSameElementsAs(expectedErrors)
        );
    }

    private static List<AttributeError> createAttributeErrors() {
        return List.of(
                new AttributeError("field1", "Error message 1"),
                new AttributeError("field1", "Error message 2"),
                new AttributeError("field2", "Error message 3"));
    }
}