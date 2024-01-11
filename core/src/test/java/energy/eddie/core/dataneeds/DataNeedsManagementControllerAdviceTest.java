package energy.eddie.core.dataneeds;

import energy.eddie.api.agnostic.exceptions.DataNeedNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DataNeedsManagementControllerAdviceTest {
    private final DataNeedsManagementControllerAdvice advice = new DataNeedsManagementControllerAdvice();

    @Test
    void givenDataNeedNotFoundException_returnsBadRequest() {
        // Given
        String dataNeedId = "dataNeedId";
        DataNeedNotFoundException notFoundException = new DataNeedNotFoundException(dataNeedId);

        // When
        ResponseEntity<String> response = advice.handleDataNeedNotFoundException(notFoundException);

        // Then
        assertNotNull(response);
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertThat(response.getBody()).contains("No dataNeed with ID '%s' found".formatted(dataNeedId));
    }
}