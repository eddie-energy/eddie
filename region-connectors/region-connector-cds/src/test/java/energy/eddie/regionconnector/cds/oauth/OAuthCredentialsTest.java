// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.oauth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OAuthCredentialsTest {

    public static Stream<Arguments> testIsValid_withInvalidRefreshToken_returnsFalse() {
        var yesterday = ZonedDateTime.now(ZoneOffset.UTC).minusDays(1);
        return Stream.of(
                Arguments.of(new OAuthCredentials("pid", "refresh-token", "access-token", yesterday)),
                Arguments.of(new OAuthCredentials("pid", "refresh-token", "access-token", null)),
                Arguments.of(new OAuthCredentials("pid", "refresh-token", null, null))
        );
    }

    @ParameterizedTest
    @MethodSource
    void testIsValid_withInvalidRefreshToken_returnsFalse(OAuthCredentials credentials) {
        // Given

        // When
        var res = credentials.isValid();

        // Then
        assertFalse(res);
    }

    @Test
    void testIsValid_withNewRefreshToken_returnsTrue() {
        // Given
        var tomorrow = ZonedDateTime.now(ZoneOffset.UTC).plusDays(1);
        var creds = new OAuthCredentials("pid", "refresh-token", "access-token", tomorrow);

        // When
        var res = creds.isValid();

        // Then
        assertTrue(res);
    }
}