package energy.eddie.regionconnector.shared.web;

import energy.eddie.api.v0.process.model.PermissionRequestState;
import energy.eddie.api.v0.process.model.validation.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class StateValidationErrorsTest {
    @Test
    void asMap_returnsMapOfErrors() {
        // Given
        ValidationException exception = new ValidationException(mock(PermissionRequestState.class), "field1", "Error message 1");
        StateValidationErrors stateValidationErrors = new StateValidationErrors(exception);

        // When
        Map<String, String> errors = stateValidationErrors.asMap();

        // Then
        assertAll(
                () -> assertEquals(1, errors.size()),
                () -> assertTrue(errors.containsKey("field1")),
                () -> assertEquals("Error message 1", errors.get("field1"))
        );
    }
}