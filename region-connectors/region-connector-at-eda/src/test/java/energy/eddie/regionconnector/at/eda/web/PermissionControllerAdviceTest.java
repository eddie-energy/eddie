package energy.eddie.regionconnector.at.eda.web;

import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.regionconnector.at.eda.services.EdaValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PermissionControllerAdviceTest {
    @Test
    void testHandleEdaValidationException_returnsExpected() {
        // Given
        var advice = new PermissionControllerAdvice();
        var expected = List.of(new AttributeError("dsoId", "invalid bla"));

        // When
        var res = advice.handleEdaValidationException(new EdaValidationException(expected));

        // Then
        assertAll(
                () -> assertEquals(expected, res.getBody()),
                () -> assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode())
        );
    }
}