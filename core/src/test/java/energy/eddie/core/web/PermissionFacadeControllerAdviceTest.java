package energy.eddie.core.web;

import energy.eddie.api.agnostic.EddieApiError;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PermissionFacadeControllerAdviceTest {

    @Test
    void testHandleException_returnsErrorMessage() {
        // Given
        var ex = new DataNeedNotFoundException("dnid");
        var advice = new PermissionFacadeControllerAdvice();

        // When
        var res = advice.handleUnknownRegionConnectorAndDataNeedNotFoundException(ex);

        // Then
        assertAll(
                () -> assertEquals(HttpStatus.NOT_FOUND, res.getStatusCode()),
                () -> assertEquals(Map.of("errors", List.of(new EddieApiError("No data need with ID 'dnid' found."))),
                                   res.getBody())
        );
    }
}