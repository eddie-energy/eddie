package energy.eddie.regionconnector.cds.client.admin;

import energy.eddie.regionconnector.cds.exceptions.NoTokenException;
import energy.eddie.regionconnector.cds.master.data.CdsServer;
import energy.eddie.regionconnector.cds.master.data.CdsServerBuilder;
import energy.eddie.regionconnector.cds.services.oauth.OAuthService;
import energy.eddie.regionconnector.cds.services.oauth.token.CredentialsWithRefreshToken;
import energy.eddie.regionconnector.cds.services.oauth.token.CredentialsWithoutRefreshToken;
import energy.eddie.regionconnector.cds.services.oauth.token.InvalidTokenResult;
import energy.eddie.regionconnector.cds.services.oauth.token.TokenResult;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminClientTest {
    private static MockWebServer server;
    private static String basepath;
    @Spy
    @SuppressWarnings("unused")
    private final WebClient webClient = WebClient.create();
    @Spy
    private final CdsServer cdsServer = new CdsServerBuilder()
            .setClientsEndpoint(basepath)
            .setAdminClientId("client-id")
            .setAdminClientSecret("client-secret")
            .build();
    @Mock
    private OAuthService oAuthService;
    @InjectMocks
    private AdminClient adminClient;

    public static Stream<Arguments> testClients_returnsClients() {
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        return Stream.of(
                Arguments.of(new CredentialsWithRefreshToken("access-token", "refresh-token", now)),
                Arguments.of(new CredentialsWithoutRefreshToken("access-token", now))
        );
    }

    @BeforeAll
    static void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        basepath = "http://localhost:" + server.getPort();
    }

    @AfterAll
    static void tearDown() throws IOException {
        server.shutdown();
    }

    @ParameterizedTest
    @MethodSource
    void testClients_returnsClients(TokenResult tokenResult) {
        // Given
        when(oAuthService.retrieveAccessToken(cdsServer)).thenReturn(tokenResult);
        // language=JSON
        var clientPage1 = """
                {
                  "next": "%s",
                  "previous": null,
                  "clients": [
                    {
                      "clientId": "client-id"
                    }
                  ]
                }
                """.formatted(basepath);
        // language=JSON
        var clientPage2 = """
                {
                  "next": null,
                  "previous": null,
                  "clients": [
                    {
                      "clientId": "client-id"
                    }
                  ]
                }
                """;

        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .addHeader("Content-Type", "application/json")
                               .setBody(clientPage1));
        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .addHeader("Content-Type", "application/json")
                               .setBody(clientPage2));

        // When
        var res = adminClient.clients();

        // Then
        StepVerifier.create(res)
                    .assertNext(list -> assertEquals(2, list.size()))
                    .verifyComplete();
    }

    @Test
    void testClients_withCachedToken_returnsClients() {
        // Given
        var expiresTomorrow = ZonedDateTime.now(ZoneOffset.UTC).plusDays(1);
        when(oAuthService.retrieveAccessToken(cdsServer))
                .thenReturn(new CredentialsWithoutRefreshToken("access-token", expiresTomorrow));
        // language=JSON
        var clientPage = """
                {
                  "next": null,
                  "previous": null,
                  "clients": [
                    {
                      "clientId": "client-id"
                    }
                  ]
                }
                """;

        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .addHeader("Content-Type", "application/json")
                               .setBody(clientPage));
        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .addHeader("Content-Type", "application/json")
                               .setBody(clientPage));

        // When
        var res = adminClient.clients()
                             .then(Mono.defer(adminClient::clients));

        // Then
        StepVerifier.create(res)
                    .expectNextCount(1)
                    .verifyComplete();
    }


    @Test
    void testClients_withoutToken_returnsNoTokenException() {
        // Given
        when(oAuthService.retrieveAccessToken(cdsServer))
                .thenReturn(new InvalidTokenResult());


        // When
        var res = adminClient.clients();

        // Then
        StepVerifier.create(res)
                    .expectError(NoTokenException.class)
                    .verify();
    }

    @Test
    void testClients_withInvalidCachedToken_requestsNewToken() {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var expiresTomorrow = now.plusDays(1);
        var expiredYesterday = now.minusDays(1);
        when(oAuthService.retrieveAccessToken(cdsServer))
                .thenReturn(new CredentialsWithoutRefreshToken("access-token", expiredYesterday))
                .thenReturn(new CredentialsWithoutRefreshToken("access-token", expiresTomorrow));
        // language=JSON
        var clientPage = """
                {
                  "next": null,
                  "previous": null,
                  "clients": [
                    {
                      "clientId": "client-id"
                    }
                  ]
                }
                """;

        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .addHeader("Content-Type", "application/json")
                               .setBody(clientPage));
        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .addHeader("Content-Type", "application/json")
                               .setBody(clientPage));

        // When
        var res = adminClient.clients()
                             .then(Mono.defer(adminClient::clients));

        // Then
        StepVerifier.create(res)
                    .expectNextCount(1)
                    .verifyComplete();
    }
}