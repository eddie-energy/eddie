// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.client;

import energy.eddie.regionconnector.es.datadis.api.DatadisApiException;
import energy.eddie.regionconnector.es.datadis.config.DatadisConfiguration;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequest;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequestResponse;
import energy.eddie.regionconnector.es.datadis.dtos.authorizations.AuthorizationStatus;
import energy.eddie.regionconnector.es.datadis.dtos.authorizations.AuthorizedCups;
import energy.eddie.regionconnector.es.datadis.dtos.authorizations.UserAuthorizationsResponse;
import io.netty.handler.codec.http.HttpHeaderValues;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("rawtypes")
class NettyAuthorizationApiClientTest {
    private static MockWebServer mockBackEnd;
    private static DatadisConfiguration datadisConfig;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeAll
    static void setUp() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
        var basePath = "http://localhost:" + mockBackEnd.getPort();
        datadisConfig = new DatadisConfiguration("username", "password", basePath);
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockBackEnd.shutdown();
    }

    @ParameterizedTest
    @MethodSource("authorizationResponses")
    void postAuthorizationRequest_withMock_returnsAuthorizationRequestResponse(
            String response,
            Class expectedResponse
    ) {
        NettyAuthorizationApiClient uut = new NettyAuthorizationApiClient(
                HttpClient.create(),
                mapper,
                () -> Mono.just("token"),
                datadisConfig
        );

        AuthorizationRequest request = new AuthorizationRequest(
                LocalDate.now(ZoneOffset.UTC),
                LocalDate.now(ZoneOffset.UTC).plusMonths(1),
                "nif",
                new ArrayList<>()
        );

        mockBackEnd.enqueue(new MockResponse()
                                    .setResponseCode(HttpStatus.OK.value())
                                    .setBody("{\"response\":\"" + response + "\"}")
                                    .addHeader("Content-Type", HttpHeaderValues.APPLICATION_JSON));


        StepVerifier.create(uut.postAuthorizationRequest(request))
                    .assertNext(actualResponse -> {
                        assertTrue(expectedResponse.isInstance(actualResponse));
                        assertEquals(response, actualResponse.originalResponse());
                    })
                    .verifyComplete();
    }

    @Test
    void postAuthorizationRequest_withInvalidToken_returnsError() {
        NettyAuthorizationApiClient uut = new NettyAuthorizationApiClient(
                HttpClient.create(),
                mapper,
                () -> Mono.just("invalid token"),
                datadisConfig
        );

        AuthorizationRequest request = new AuthorizationRequest(
                LocalDate.now(ZoneOffset.UTC),
                LocalDate.now(ZoneOffset.UTC).plusMonths(1),
                "nif",
                new ArrayList<>()
        );

        mockBackEnd.enqueue(new MockResponse()
                                    .setResponseCode(HttpStatus.UNAUTHORIZED.value())
                                    .setBody(
                                            "{\"timestamp\":\"2024-01-30T11:46:39.299+0000\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"No message available\",\"path\":\"/api-private/request/send-request-authorization\"}")
                                    .addHeader("Content-Type", HttpHeaderValues.APPLICATION_JSON));

        StepVerifier.create(uut.postAuthorizationRequest(request))
                    .expectError(DatadisApiException.class)
                    .verify();
    }

    @Test
    void getThirdPartyAuthorizedUsersCups_returnsAuthorizedUsersCups() {
        // Given
        // language=JSON
        var payload = """
                {
                  "response": [
                    {
                      "id": 7330921,
                      "ownerDocument": "G00000000",
                      "ownerName": "John Doe",
                      "ownerDocumentTypeName": "NIF",
                      "requesterDocument": "00000000",
                      "requesterDocumentTypeName": "NIF",
                      "requesterName": "EDDIE Framework",
                      "requestId": 1521447,
                      "cups": "ES0000000000000000GG",
                      "status": {
                        "id": 5,
                        "description": "VIGENTE"
                      },
                      "validityDateStart": "2026-03-30T00:00:00.000+0200",
                      "validityDateEnd": "2026-03-31T23:59:59.000+0200",
                      "isDeleted": null,
                      "distributorCodeFather": "0000"
                    }
                  ]
                }
                """;

        NettyAuthorizationApiClient uut = new NettyAuthorizationApiClient(
                HttpClient.create(),
                mapper,
                () -> Mono.just("token"),
                datadisConfig
        );

        mockBackEnd.enqueue(new MockResponse()
                                    .setResponseCode(HttpStatus.OK.value())
                                    .setBody(payload)
                                    .addHeader("Content-Type", HttpHeaderValues.APPLICATION_JSON));
        var expected = new UserAuthorizationsResponse(List.of(
                new AuthorizedCups(
                        7330921L,
                        "G00000000",
                        "John Doe",
                        "NIF",
                        "00000000",
                        "NIF",
                        "EDDIE Framework",
                        1521447L,
                        "ES0000000000000000GG",
                        AuthorizationStatus.CURRENT,
                        ZonedDateTime.parse("2026-03-30T00:00:00.000+02").withZoneSameInstant(ZoneOffset.UTC),
                        ZonedDateTime.parse("2026-03-31T23:59:59.000+02").withZoneSameInstant(ZoneOffset.UTC),
                        null,
                        "0000"
                ))
        );

        // When
        var response = uut.getThirdPartyAuthorizedUsersCups();

        // Then
        StepVerifier.create(response)
                    .expectNext(expected)
                    .verifyComplete();
    }


    @Test
    void getThirdPartyAuthorizedUsersCups_withInvalidToken_returnsError() {
        NettyAuthorizationApiClient uut = new NettyAuthorizationApiClient(
                HttpClient.create(),
                mapper,
                () -> Mono.just("invalid token"),
                datadisConfig
        );

        mockBackEnd.enqueue(new MockResponse()
                                    .setResponseCode(HttpStatus.UNAUTHORIZED.value())
                                    .setBody(
                                            "{\"timestamp\":\"2024-01-30T11:46:39.299+0000\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"No message available\",\"path\":\"/api-private/request/send-request-authorization\"}")
                                    .addHeader("Content-Type", HttpHeaderValues.APPLICATION_JSON));

        StepVerifier.create(uut.getThirdPartyAuthorizedUsersCups())
                    .expectError(DatadisApiException.class)
                    .verify();
    }

    private static Stream<Arguments> authorizationResponses() {
        return Stream.of(
                Arguments.of("ok", AuthorizationRequestResponse.Ok.class),
                Arguments.of("nonif", AuthorizationRequestResponse.NoNif.class),
                Arguments.of("nopermisos", AuthorizationRequestResponse.NoPermission.class),
                Arguments.of("xxx", AuthorizationRequestResponse.Unknown.class)

        );
    }
}