package energy.eddie.regionconnector.dk.energinet.web;

import energy.eddie.api.v0.process.model.PastStateException;
import energy.eddie.api.v0.process.model.PermissionRequestState;
import energy.eddie.api.v0.process.model.SendToPermissionAdministratorException;
import energy.eddie.api.v0.process.model.validation.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class PermissionRequestControllerAdviceTest {
    private final PermissionRequestControllerAdvice advice = new PermissionRequestControllerAdvice();
    @Mock
    private PermissionRequestState mockState;

    @Test
    void givenPastStateException_returnsInternalServerError() {
        ResponseEntity<Object> response = advice.handleStateTransitionException(new PastStateException(mockState));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertThat(response.toString()).contains("An error occurred while trying to transition a permission request to a new state");
    }

    @Test
    void givenSendToPermissionAdministratorException_returnsInternalServerError() {
        // Given
        var exception = new SendToPermissionAdministratorException(mockState, "This is a message", false);

        // When
        ResponseEntity<Object> response = advice.handleSendToPermissionAdministratorException(exception);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertThat(response.toString()).contains("This is a message");
    }

    @Test
    void givenSendToPermissionAdministratorExceptionWithUserFault_returnsBadRequest() {
        // Given
        var exception = new SendToPermissionAdministratorException(mockState, "This is a message", true);

        // When
        ResponseEntity<Object> response = advice.handleSendToPermissionAdministratorException(exception);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertThat(response.toString()).contains("This is a message");
    }

    @Test
    void givenValidationException_returnsBadRequest() {
        // Given
        var exception = new ValidationException(mockState, "field", "error");

        // When
        ResponseEntity<Object> response = advice.handleValidationException(exception);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertThat(response.toString()).contains("field", "error");
    }
}