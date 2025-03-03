package energy.eddie.regionconnector.cds.services.oauth.token;

import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CredentialsWithoutRefreshTokenTest {

    @Test
    void testIsValid_returnsTrue() {
        // Given
        var expiresTomorrow = ZonedDateTime.now(ZoneOffset.UTC).plusDays(1);
        var creds = new CredentialsWithoutRefreshToken("asdf", expiresTomorrow);

        // When
        var res = creds.isValid();

        // Then
        assertTrue(res);
    }

    @Test
    void testIsValid_returnsFalse() {
        // Given
        var expiredYesterday = ZonedDateTime.now(ZoneOffset.UTC).minusDays(1);
        var creds = new CredentialsWithoutRefreshToken("asdf", expiredYesterday);

        // When
        var res = creds.isValid();

        // Then
        assertFalse(res);
    }
}