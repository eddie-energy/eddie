package energy.eddie.regionconnector.es.datadis.client;

import energy.eddie.regionconnector.es.datadis.api.DatadisApiException;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequest;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequestResponse;
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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("rawtypes")
class NettyAuthorizationApiClientTest {
    static MockWebServer mockBackEnd;
    static DatadisEndpoints datadisEndpoints;

    @BeforeAll
    static void setUp() throws IOException, URISyntaxException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
        URI endpoint = new URI("http://localhost:" + mockBackEnd.getPort());
        datadisEndpoints = mock(DatadisEndpoints.class);
        when(datadisEndpoints.authorizationRequestEndpoint()).thenReturn(endpoint);
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockBackEnd.shutdown();
    }

    private static Stream<Arguments> authorizationResponses() {
        return Stream.of(
                Arguments.of("ok", AuthorizationRequestResponse.Ok.class),
                Arguments.of("nonif", AuthorizationRequestResponse.NoNif.class),
                Arguments.of("nopermisos", AuthorizationRequestResponse.NoPermission.class),
                Arguments.of("xxx", AuthorizationRequestResponse.Unknown.class)

        );
    }

    @ParameterizedTest
    @MethodSource("authorizationResponses")
    void postAuthorizationRequest_withMock_returnsAuthorizationRequestResponse(String response, Class expectedResponse) {
        NettyAuthorizationApiClient uut = new NettyAuthorizationApiClient(
                HttpClient.create(),
                () -> Mono.just("token"),
                datadisEndpoints);

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
                () -> Mono.just("invalid token"),
                datadisEndpoints);

        AuthorizationRequest request = new AuthorizationRequest(
                LocalDate.now(ZoneOffset.UTC),
                LocalDate.now(ZoneOffset.UTC).plusMonths(1),
                "nif",
                new ArrayList<>()
        );

        mockBackEnd.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.UNAUTHORIZED.value())
                .setBody("{\"timestamp\":\"2024-01-30T11:46:39.299+0000\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"No message available\",\"path\":\"/api-private/request/send-request-authorization\"}")
                .addHeader("Content-Type", HttpHeaderValues.APPLICATION_JSON));

        StepVerifier.create(uut.postAuthorizationRequest(request))
                .expectError(DatadisApiException.class)
                .verify();
    }
}