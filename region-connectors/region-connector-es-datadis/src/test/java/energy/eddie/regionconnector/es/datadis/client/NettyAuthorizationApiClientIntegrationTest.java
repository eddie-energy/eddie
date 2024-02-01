package energy.eddie.regionconnector.es.datadis.client;

import energy.eddie.regionconnector.es.datadis.api.DatadisApiException;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;

class NettyAuthorizationApiClientIntegrationTest {
    String requestNif = "replace_me";
    String token = "replace_me";

    String basePath = "https://datadis.es";

    @Test
    @Disabled("Integration test, that needs real credentials")
    void postAuthorizationRequest_withValidInput_doesNotReturnError() {
        NettyAuthorizationApiClient uut = new NettyAuthorizationApiClient(
                HttpClient.create(),
                new MyTokenProvider(),
                basePath);

        AuthorizationRequest request = new AuthorizationRequest(
                LocalDate.now(ZoneOffset.UTC),
                LocalDate.now(ZoneOffset.UTC).plusMonths(1),
                requestNif,
                new ArrayList<>()
        );

        StepVerifier.create(uut.postAuthorizationRequest(request))
                .verifyComplete();
    }

    @Test
    void postAuthorizationRequest_withInvalidToken_returnsError() {
        NettyAuthorizationApiClient uut = new NettyAuthorizationApiClient(
                HttpClient.create(),
                () -> Mono.just("invalid token"),
                basePath);

        AuthorizationRequest request = new AuthorizationRequest(
                LocalDate.now(ZoneOffset.UTC),
                LocalDate.now(ZoneOffset.UTC).plusMonths(1),
                requestNif,
                new ArrayList<>()
        );

        StepVerifier.create(uut.postAuthorizationRequest(request))
                .expectError(DatadisApiException.class)
                .verify();
    }

    @Test
    @Disabled("Integration test, that needs real credentials")
    void postAuthorizationRequest_withInvalidNif_returnsError() {
        NettyAuthorizationApiClient uut = new NettyAuthorizationApiClient(
                HttpClient.create(),
                new MyTokenProvider(),
                basePath);

        AuthorizationRequest request = new AuthorizationRequest(
                LocalDate.now(ZoneOffset.UTC),
                LocalDate.now(ZoneOffset.UTC).plusMonths(1),
                "invalid nif",
                new ArrayList<>()
        );

        StepVerifier.create(uut.postAuthorizationRequest(request))
                .expectError(DatadisApiException.class)
                .verify();
    }

    class MyTokenProvider implements DatadisTokenProvider {

        @Override
        public Mono<String> getToken() {
            return Mono.just(token);
        }
    }
}