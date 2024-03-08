package energy.eddie.regionconnector.fr.enedis.client;

import energy.eddie.regionconnector.fr.enedis.config.PlainEnedisConfiguration;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnedisTokenProviderTest {

    public static final String VALID_TOKEN_1 = """
            {
                "access_token": "token1",
                "scope": "am_application_scope default",
                "token_type": "Bearer",
                "expires_in": 12600
            }
            """;
    public static final String VALID_TOKEN_2 = """
            {
                "access_token": "token2",
                "scope": "am_application_scope default",
                "token_type": "Bearer",
                "expires_in": 12600
            }
            """;
    static MockWebServer mockBackEnd;
    private static String basePath;
    private static WebClient webClient;

    private static Stream<Arguments> invalidCredentialResponses() {
        return Stream.of(
                Arguments.of("""
                                {
                                    "error_description": "Client Authentication failed.",
                                    "error": "invalid_client"
                                }
                                """,
                        "invalid secret"),
                Arguments.of("""
                                {
                                    "error_description": "A valid OAuth client could not be found for client_id: ZRJbXYFSMRYmlHbKsW45j5p08PU",
                                    "error": "invalid_client"
                                }
                                """,
                        "client id does not exist")
        );
    }

    @BeforeEach
    void setUp() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
        basePath = "http://localhost:" + mockBackEnd.getPort();
        webClient = WebClient.builder()
                .baseUrl(basePath)
                .build();
    }

    @Test
    void getToken_fetchesTokenIfExpired() {
        EnedisTokenProvider uut = new EnedisTokenProvider(
                new PlainEnedisConfiguration("id", "secret", basePath, 24),
                webClient
        );

        mockBackEnd.enqueue(new MockResponse()
                .setBody("""
                        {
                            "access_token": "token1",
                            "scope": "am_application_scope default",
                            "token_type": "Bearer",
                            "expires_in": -100
                        }
                        """)
                .addHeader("Content-Type", "application/json"));

        mockBackEnd.enqueue(new MockResponse()
                .setBody(VALID_TOKEN_2)
                .addHeader("Content-Type", "application/json"));

        StepVerifier.create(uut.getToken())
                .assertNext(token -> assertEquals("token1", token))
                .verifyComplete();

        StepVerifier.create(uut.getToken())
                .assertNext(token -> assertEquals("token2", token))
                .verifyComplete();
    }

    @Test
    void getToken_doesNotFetchTokenIfItIsStillValid() {
        EnedisTokenProvider uut = new EnedisTokenProvider(
                new PlainEnedisConfiguration("id", "secret", basePath, 24),
                webClient
        );

        mockBackEnd.enqueue(new MockResponse()
                .setBody(VALID_TOKEN_1)
                .addHeader("Content-Type", "application/json"));

        mockBackEnd.enqueue(new MockResponse()
                .setBody(VALID_TOKEN_2)
                .addHeader("Content-Type", "application/json"));

        StepVerifier.create(uut.getToken())
                .assertNext(token -> assertEquals("token1", token))
                .verifyComplete();

        StepVerifier.create(uut.getToken())
                .assertNext(token -> assertEquals("token1", token))
                .verifyComplete();
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("invalidCredentialResponses")
    void getToken_withInvalidCredentials_returnsError(String response, String name) {
        EnedisTokenProvider uut = new EnedisTokenProvider(
                new PlainEnedisConfiguration("x", "x", basePath, 24),
                webClient
        );

        mockBackEnd.enqueue(new MockResponse()
                .setResponseCode(401)
                .setBody(response)
                .addHeader("Content-Type", "application/json"));

        StepVerifier.create(uut.getToken())
                .expectError(TokenProviderException.class)
                .verify();
    }
}