package energy.eddie.core.dataneeds;

import energy.eddie.api.agnostic.EddieApiError;
import energy.eddie.core.dataneeds.exceptions.DataNeedAlreadyExistsException;
import energy.eddie.core.dataneeds.exceptions.DataNeedIdsNotMatchingException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static energy.eddie.api.agnostic.GlobalConfig.ERRORS_PROPERTY_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DataNeedsManagementControllerAdviceTest {
    private final DataNeedsManagementControllerAdvice advice = new DataNeedsManagementControllerAdvice();

    @Test
    void givenDataNeedAlreadyExistsException_returnsConflict() {
        // Given
        var dataNeedId = "dataNeedId";
        var exception = new DataNeedAlreadyExistsException(dataNeedId);
        var expectedError = "Data need with ID '%s' already exists.".formatted(dataNeedId);

        // When
        ResponseEntity<Map<String, List<EddieApiError>>> response = advice.handleDataNeedAlreadyExistsException(exception);

        // Then
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        var responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(1, responseBody.size());
        assertEquals(1, responseBody.get(ERRORS_PROPERTY_NAME).size());
        assertEquals(expectedError, responseBody.get(ERRORS_PROPERTY_NAME).getFirst().message());
    }

    @Test
    void givenDataNeedIdsNotMatchingException_returnsBadRequest() {
        // Given
        var exception = new DataNeedIdsNotMatchingException();

        // When
        ResponseEntity<Map<String, List<EddieApiError>>> response = advice.handleDataNeedIdsNotMatchingException(exception);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        var responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(1, responseBody.size());
        assertEquals(1, responseBody.get(ERRORS_PROPERTY_NAME).size());
        assertEquals("Data need ID in URL does not match data need ID in request body.", responseBody.get(ERRORS_PROPERTY_NAME).getFirst().message());
    }
}
