// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messenger;

import energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapterConfiguration;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class WebClientPontonTokenProviderTest {

    private static final String TOKEN_1 = "eyJhbGciOiJIUzUxMiJ9.eyJTQUxUIjoiZTczM2VmNDctN2Q0OC00NGY0LTlmNGMtZDcwOTZlNGU2NTUwIiwicmVtb3RlSVAiOiIxNzIuMTYuMjEuMjEzIiwiZm9yd2FyZGVkRm9yIjoiIiwic3ViIjoidXNlciIsImlhdCI6MTcyMTcyNzkxNywiZXhwIjoxNzIxNzQ1OTE3fQ.FmQ6dLFSjJIZn3bQPwy-MIy2fltmXjrughWNdD0TyveGtTVEUwLRVi0eEhOtOg-Xqx8kJHvEbKvVvnZsrPy4Nw";
    public static final String RESPONSE_1 = """
            {
                "username": "user",
                "setupTwoFactor": false,
                "token": "%s"
            }
            """.formatted(TOKEN_1);
    private static final String TOKEN_2 = "eyJhbGciOiJIUzUxMiJ9.eyJTQUxUIjoiMzJmOGFhNDItZTJkZC00ODRiLTg4YmMtODgwMjMyMzlhMTQwIiwicmVtb3RlSVAiOiIxNzIuMTYuMjEuNTgiLCJmb3J3YXJkZWRGb3IiOiIiLCJzdWIiOiJ1c2VyIiwiaWF0IjoxNzIxODkzNTQ5LCJleHAiOjE3MjE5MTE1NDl9.H0D1f3o21VDHIB0HemCGh35LfMgfrPDwAuMD-EnouRKkkP0aUWjDPdvIq7BPFIaS8UyjxBc0GfaPfw13ktqJ4Q";
    public static final String RESPONSE_2 = """
            {
                "username": "user",
                "setupTwoFactor": false,
                "token": "%s"
            }
            """.formatted(TOKEN_2);
    static MockWebServer mockBackEnd;

    private static PontonXPAdapterConfiguration config;
    private final WebClient webClient = WebClient.create();

    @BeforeAll
    static void setUp() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
        config = new PontonXPAdapterConfiguration(
                "adapterId",
                "adapterVersion",
                "hostname",
                1234,
                "http://localhost:" + mockBackEnd.getPort(),
                "folder",
                "user",
                "password"
        );
    }

    @Test
    void getToken_fetchesTokenFromServer() {
        // Arrange
        var tokenProvider = new WebClientPontonTokenProvider(webClient, config);

        mockBackEnd.enqueue(new MockResponse()
                                    .setBody(RESPONSE_1)
                                    .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        );

        // Act
        var token = tokenProvider.getToken().block();

        // Assert
        assertEquals(TOKEN_1, token);
    }

    @Test
    void getToken_fetchesTokenFromServer_whenTokenIsAlreadyExpired() {
        // Arrange
        var tokenProvider = new WebClientPontonTokenProvider(webClient, config);

        mockBackEnd.enqueue(new MockResponse()
                                    .setBody(RESPONSE_1)
                                    .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        );
        mockBackEnd.enqueue(new MockResponse()
                                    .setBody(RESPONSE_2)
                                    .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        );

        // Act
        var firstToken = tokenProvider.getToken().block();
        var secondToken = tokenProvider.getToken().block();

        // Assert
        assertAll(
                () -> assertEquals(TOKEN_1, firstToken),
                () -> assertEquals(TOKEN_2, secondToken)
        );
    }
}
