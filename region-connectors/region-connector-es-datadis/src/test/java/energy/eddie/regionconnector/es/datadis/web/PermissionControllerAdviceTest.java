package energy.eddie.regionconnector.es.datadis.web;

import energy.eddie.api.v0.process.model.PastStateException;
import energy.eddie.api.v0.process.model.PermissionRequestState;
import energy.eddie.regionconnector.es.datadis.dtos.exceptions.PermissionNotFoundException;
import energy.eddie.regionconnector.es.datadis.permission.request.state.RejectedState;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PermissionControllerAdviceTest {
    private final PermissionControllerAdvice advice = new PermissionControllerAdvice();
    @Mock
    private PermissionRequestState mockState;

    @Test
    void givenPastStateException_returnsInternalServerError() {
        // Given
        RejectedState state = new RejectedState(null);

        // When
        ResponseEntity<Object> response = advice.handleStateTransitionException(new PastStateException(state));

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertThat(response.toString()).contains("An error occurred while trying to transition a permission request to a new state");
    }

    @Test
    void givenPermissionNotFoundException_returnsNotFound() {
        // Given
        var permissionId = "SomePermissionId";
        var exception = new PermissionNotFoundException(permissionId);

        // When
        ResponseEntity<Object> response = advice.handlePermissionNotFoundException(exception);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertThat(response.toString()).contains(permissionId);
    }

    // TODO test other methods
//    @Test
//    void givenValidationException_returnsBadRequest() {
//        // Given
//        var exception = new ValidationException(mockState, "field", "error");
//
//        // When
//        ResponseEntity<Object> response = advice.handleValidationException(exception);
//
//        // Then
//        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
//        assertThat(response.toString()).contains("field", "error");
//    }
}