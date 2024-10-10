package energy.eddie.regionconnector.shared.jwt;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


class JwtValidationsTest {
    private static final String VALID_REFRESH_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwiaWF0Ijo1NTE2MjM5MDIyLCJleHAiOjU1MTYyMzkwMjJ9.Gce4NCqCL64_1GvTP7gVzHkyC4kXEG0RAgAfxfNdVno";
    private static final String INVALID_REFRESH_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwiaWF0Ijo1MTYyMzkwMjIsImV4cCI6NTE2MjM5MDIyfQ.1JANCZUSNrVsGYTN7hcHE3EI3RpycwRwsQvdkv4EV3A";

    @Test
    void isValidUntil_returnsTrueForValidRefreshToken() {
        // Given
        var data = LocalDate.now(ZoneOffset.UTC).plusDays(10);

        // When
        var res = JwtValidations.isValidUntil(VALID_REFRESH_TOKEN, data);

        // Then
        assertTrue(res);
    }

    @Test
    void isValidUntil_returnsTrueForInvalidRefreshToken() {
        // Given
        var data = LocalDate.now(ZoneOffset.UTC).plusDays(10);

        // When
        var res = JwtValidations.isValidUntil(INVALID_REFRESH_TOKEN, data);

        // Then
        assertFalse(res);
    }
}