package energy.eddie.regionconnector.de.eta.oauth;

import energy.eddie.regionconnector.de.eta.config.DeEtaPlusConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EtaOAuthServiceTest {

    @Mock
    private DeEtaPlusConfiguration configuration;

    @Mock
    private WebClient.Builder builder;

    private final DeEtaPlusConfiguration.OAuthConfig oauthConfig = new DeEtaPlusConfiguration.OAuthConfig(
            "client", "secret", "http://token.url", "http://auth.url", "http://redirect.uri", "scope");

    @Test
    void exchangeCodeForTokenShouldCallWebClientProperly() {
        when(configuration.oauth()).thenReturn(oauthConfig);

        ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);
        ClientResponse response = ClientResponse.create(org.springframework.http.HttpStatus.OK)
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE,
                        org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
                .body("{\"success\": true, \"data\": {\"token\": \"acc-token\", \"refreshToken\": \"ref-token\"}}")
                .build();
        when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(Mono.just(response));

        WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
        when(builder.build()).thenReturn(webClient);

        EtaOAuthService service = new EtaOAuthService(builder, configuration);

        Mono<OAuthTokenResponse> resultMono = service.exchangeCodeForToken("auth-code", "client-id");

        StepVerifier.create(resultMono)
                .assertNext(res -> {
                    assertThat(res.success()).isTrue();
                    assertThat(res.getAccessToken()).isEqualTo("acc-token");
                    assertThat(res.getRefreshToken()).isEqualTo("ref-token");
                })
                .verifyComplete();
    }

    @Test
    void exchangeCodeForTokenWhenUnsuccessfulResponseShouldNotFailInProcessing() {
        when(configuration.oauth()).thenReturn(oauthConfig);

        ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);
        ClientResponse response = ClientResponse.create(org.springframework.http.HttpStatus.OK)
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE,
                        org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
                .body("{\"success\": false}")
                .build();
        when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(Mono.just(response));

        WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
        when(builder.build()).thenReturn(webClient);

        EtaOAuthService service = new EtaOAuthService(builder, configuration);

        Mono<OAuthTokenResponse> resultMono = service.exchangeCodeForToken("auth-code", "client-id");

        StepVerifier.create(resultMono)
                .assertNext(res -> {
                    assertThat(res.success()).isFalse();
                    assertThat(res.getAccessToken()).isNull();
                })
                .verifyComplete();
    }
}
