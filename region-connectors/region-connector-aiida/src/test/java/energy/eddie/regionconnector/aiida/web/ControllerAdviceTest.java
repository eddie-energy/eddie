// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.web;

import energy.eddie.regionconnector.aiida.exceptions.CredentialsAlreadyExistException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static energy.eddie.api.agnostic.GlobalConfig.ERRORS_PROPERTY_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ControllerAdviceTest {
    private final ControllerAdvice advice = new ControllerAdvice();

    @Test
    void givenCredentialsAlreadyExistException_returnsBadRequest() {
        // Given
        var exception = new CredentialsAlreadyExistException("testId");

        // When
        var response = advice.handleCredentialsAlreadyExistException(exception);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        var responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(1, responseBody.size());
        assertEquals(1, responseBody.get(ERRORS_PROPERTY_NAME).size());
        assertEquals(
                "MQTT credentials for permission 'testId' have already been created and fetched, and this is only permitted once",
                responseBody.get(ERRORS_PROPERTY_NAME).getFirst().message());
    }
}
