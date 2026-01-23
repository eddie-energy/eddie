// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.client;

import energy.eddie.regionconnector.es.datadis.api.DatadisApiException;
import energy.eddie.regionconnector.es.datadis.config.DatadisConfiguration;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;

class NettyAuthorizationApiClientIntegrationTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final String requestNif = "replace_me";
    private final DatadisConfiguration config = new DatadisConfiguration("username", "password", "https://datadis.es");

    @Test
    @Disabled("Integration test, that needs real credentials")
    void postAuthorizationRequest_withValidInput_doesNotReturnError() {
        NettyAuthorizationApiClient uut = new NettyAuthorizationApiClient(
                HttpClient.create(),
                mapper,
                new MyTokenProvider(),
                config
        );

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
                mapper,
                () -> Mono.just("invalid token"),
                config
        );

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
                mapper,
                new MyTokenProvider(),
                config
        );

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

    static class MyTokenProvider implements DatadisTokenProvider {

        @Override
        public Mono<String> getToken() {
            return Mono.just("replace_me");
        }
    }
}