// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.oauth;

import com.nimbusds.oauth2.sdk.ParseException;
import energy.eddie.regionconnector.be.fluvius.config.FluviusOAuthConfiguration;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OAuthTokenServiceTest {
    private static final String VALID_REFRESH_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwiaWF0Ijo1NTE2MjM5MDIyLCJleHAiOjU1MTYyMzkwMjJ9.Gce4NCqCL64_1GvTP7gVzHkyC4kXEG0RAgAfxfNdVno";
    public static final String PAYLOAD = """
            {
              "token_type": "Bearer",
              "expires_in": 3599,
              "ext_expires_in": 3599,
              "access_token": "%s"
            }
            """.formatted(VALID_REFRESH_TOKEN);
    private static final String INVALID_REFRESH_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwiaWF0Ijo1MTYyMzkwMjIsImV4cCI6NTE2MjM5MDIyfQ.1JANCZUSNrVsGYTN7hcHE3EI3RpycwRwsQvdkv4EV3A";
    private static final MockWebServer SERVER = new MockWebServer();
    private static FluviusOAuthConfiguration config;

    @BeforeAll
    static void setUp() throws IOException {
        SERVER.start();
        config = new FluviusOAuthConfiguration(
                "http://localhost:" + SERVER.getPort(),
                "client-id",
                "client-secret",
                "tenant-id",
                "scope"
        );
    }


    @Test
    void testAccessToken_withValidToken() throws OAuthException, URISyntaxException, ParseException, IOException {
        // Given
        var service = new OAuthTokenService(config, VALID_REFRESH_TOKEN);

        // When
        var res = service.accessToken();

        // Then
        assertEquals(VALID_REFRESH_TOKEN, res);
    }

    @Test
    void testAccessToken_withInvalidToken_requestsNewToken() throws OAuthException, URISyntaxException, IOException, ParseException {
        // Given
        var service = new OAuthTokenService(config, INVALID_REFRESH_TOKEN);
        SERVER.enqueue(
                new MockResponse()
                        .setBody(PAYLOAD)
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
        );

        // When
        var res = service.accessToken();

        // Then
        assertEquals(VALID_REFRESH_TOKEN, res);
    }

    @Test
    void testAccessToken_withoutToken_requestsNewToken() throws OAuthException, URISyntaxException, IOException, ParseException {
        // Given
        var service = new OAuthTokenService(config);
        SERVER.enqueue(
                new MockResponse()
                        .setBody(PAYLOAD)
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
        );

        // When
        var res = service.accessToken();

        // Then
        assertEquals(VALID_REFRESH_TOKEN, res);
    }

    @Test
    void testAccessToken_withInvalidResponse_throws() {
        // Given
        var service = new OAuthTokenService(config);
        SERVER.enqueue(
                new MockResponse()
                        .setBody(PAYLOAD)
                        .setResponseCode(401)
                        .setHeader("Content-Type", "application/json")
        );

        // When & Then
        assertThrows(OAuthException.class, service::accessToken);
    }
}