package energy.eddie.dataneeds.web;

import energy.eddie.api.agnostic.EddieApiError;
import energy.eddie.api.agnostic.exceptions.DataNeedNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static energy.eddie.api.agnostic.GlobalConfig.ERRORS_PROPERTY_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DataNeedsAdviceTest {
    private final DataNeedsAdvice advice = new DataNeedsAdvice();

    @Test
    void givenDataNeedNotFoundException_returnsNotFound() {
        // Given
        var exception = new DataNeedNotFoundException("some-non-existing-id", false);

        // When
        ResponseEntity<Map<String, List<EddieApiError>>> response = advice.handleDataNeedNotFoundException(exception);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        var responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(1, responseBody.size());
        assertEquals(1, responseBody.get(ERRORS_PROPERTY_NAME).size());
        assertEquals("No data need with ID 'some-non-existing-id' found.",
                     responseBody.get(ERRORS_PROPERTY_NAME).getFirst().message());
    }

    @Test
    void givenDataNeedNotFoundException_returnsBadRequestIfBadRequest() {
        // Given
        var exception = new DataNeedNotFoundException("some-non-existing-id");

        // When
        ResponseEntity<Map<String, List<EddieApiError>>> response = advice.handleDataNeedNotFoundException(exception);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        var responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(1, responseBody.size());
        assertEquals(1, responseBody.get(ERRORS_PROPERTY_NAME).size());
        assertEquals("No data need with ID 'some-non-existing-id' found.",
                     responseBody.get(ERRORS_PROPERTY_NAME).getFirst().message());
    }
}
