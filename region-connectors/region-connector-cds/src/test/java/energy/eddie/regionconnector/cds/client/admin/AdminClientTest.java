// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.client.admin;

import energy.eddie.regionconnector.cds.client.JsonResponses;
import energy.eddie.regionconnector.cds.dtos.CdsServerRedirectUriUpdate;
import energy.eddie.regionconnector.cds.openapi.model.ListingCredentials200ResponseCredentialsInner;
import energy.eddie.regionconnector.cds.services.oauth.token.CredentialsWithoutRefreshToken;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.net.URI;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AdminClientTest {
    private static MockWebServer server;
    private static URI baseUri;
    private final CredentialsWithoutRefreshToken token = new CredentialsWithoutRefreshToken(
            "access-token",
            ZonedDateTime.now(ZoneOffset.UTC).plusDays(1)
    );
    @Spy
    @SuppressWarnings("unused")
    private final WebClient.Builder webClientBuilder = WebClient.builder();
    @InjectMocks
    private AdminClient adminClient;

    @BeforeAll
    static void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        baseUri = server.url("/").uri();
    }

    @AfterAll
    static void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void testClients_returnsClients() {
        // Given
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
                """.formatted(baseUri);
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
        var res = adminClient.clients(baseUri, token);

        // Then
        StepVerifier.create(res)
                    .assertNext(clients -> assertThat(clients).hasSize(2))
                    .verifyComplete();
    }

    @Test
    void testModifyClient_returnsNewClient() {
        // Given
        // language=JSON
        var client = """
                {
                  "clientId": "client-id"
                }
                """;

        server.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .addHeader("Content-Type", "application/json")
                               .setBody(client));

        // When
        var res = adminClient.modifyClient(new CdsServerRedirectUriUpdate(List.of()), baseUri, token);

        // Then
        StepVerifier.create(res)
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void testCredentials_returnsCredentials() {
        // Given
        // language=JSON
        var body = """
                {
                  "credentials": [
                    {
                      "client_id": "client-id",
                      "client_secret": "secret",
                      "client_secret_expires_at": 0,
                      "created": "2025-03-06T09:01:25.085033+00:00",
                      "credential_id": "b7b1edf0-fb17-48bc-926b-bfcc9731fc08",
                      "modified": "2025-03-06T09:01:25.085042+00:00",
                      "type": "client_secret",
                      "uri": "https://s-1cd67f6d.cds.utilityapi.com/api/cds/v1/credentials/client-id"
                    }
                  ],
                  "next": null,
                  "previous": null
                }
                """;
        server.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(body)
        );

        // When
        var res = adminClient.credentials("client-id", baseUri, token);

        // Then
        StepVerifier.create(res)
                    .assertNext(creds -> assertThat(creds.getCredentials())
                            .singleElement()
                            .extracting(ListingCredentials200ResponseCredentialsInner::getClientId,
                                        ListingCredentials200ResponseCredentialsInner::getClientSecret)
                            .isEqualTo(List.of("client-id", "secret")))
                    .verifyComplete();
    }

    @Test
    void testCarbonDataSpec_returnsCarbonDataSpec() {
        // Given
        server.enqueue(
                new MockResponse()
                        .setHeader("Content-Type", "application/json")
                        .setBody(JsonResponses.CDS_METADATA_RESPONSE)
                        .setResponseCode(200)
        );

        // When
        var res = adminClient.carbonDataSpec(baseUri, token);

        // Then
        StepVerifier.create(res)
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void testOauthMetadataSpec_returnsOAuthMetadataSpec() {
        // Given
        server.enqueue(
                new MockResponse()
                        .setHeader("Content-Type", "application/json")
                        .setBody(JsonResponses.OAUTH_METADATA_RESPONSE)
                        .setResponseCode(200)
        );

        // When
        var res = adminClient.oauthMetadataSpec(baseUri, token);

        // Then
        StepVerifier.create(res)
                    .expectNextCount(1)
                    .verifyComplete();
    }
}