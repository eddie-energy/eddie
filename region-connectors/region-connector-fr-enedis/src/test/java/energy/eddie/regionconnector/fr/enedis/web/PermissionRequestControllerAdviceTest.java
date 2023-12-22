package energy.eddie.regionconnector.fr.enedis.web;

import energy.eddie.api.v0.process.model.PastStateException;
import energy.eddie.api.v0.process.model.PermissionRequestState;
import energy.eddie.api.v0.process.model.states.AcceptedPermissionRequestState;
import energy.eddie.api.v0.process.model.validation.ValidationException;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
class PermissionRequestControllerAdviceTest {
    @Test
    void onTransitionException_returnsBadRequest() {
        // Given
        var advice = new PermissionRequestControllerAdvice();

        // When
        var res = advice.stateTransitionException(new PastStateException(AcceptedPermissionRequestState.class));

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, res.getStatusCode());
    }

    @Test
    void onValidationException_returnsBadRequest() {
        // Given
        var advice = new PermissionRequestControllerAdvice();

        // When
        var res = advice.handleStateValidationExceptions(new ValidationException(mock(PermissionRequestState.class), "field", "message"));

        // Then
        assertEquals(Map.of("field", "message"), res);
    }

    @Test
    void onMethodArgumentNotValidException_returnsBadRequest() {
        // Given
        var advice = new PermissionRequestControllerAdvice();
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getAllErrors()).thenReturn(List.of(new FieldError("field", "field", "Error message")));
        var ex = new MethodArgumentNotValidException(mock(MethodParameter.class), bindingResult);

        // When
        var res = advice.handleValidationExceptions(ex);

        // Then
        assertEquals(Map.of("field", "Error message"), res);
    }

    @Test
    void onPermissionRequestNotFoundException_returnsNotFound() {
        // Given
        var advice = new PermissionRequestControllerAdvice();

        // When
        var res = advice.handlePermissionRequestNotFound(new PermissionNotFoundException("pid"));

        // Then
        assertEquals(HttpStatus.NOT_FOUND, res.getStatusCode());
    }

}