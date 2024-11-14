package energy.eddie.regionconnector.fr.enedis.client;

import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.dto.auth.TokenResponse;
import jakarta.annotation.Nullable;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

public class EnedisTokenProvider {
    private final WebClient webClient;
    private final EnedisConfiguration configuration;
    @Nullable
    private String token;
    private long expiryTime;

    public EnedisTokenProvider(EnedisConfiguration configuration, WebClient webClient) {
        this.webClient = webClient;
        this.configuration = configuration;
    }

    /**
     * Retrieves and caches a token. If the token is expired, a new token is fetched.
     * If the token cannot be fetched the mono errors with a {@link TokenProviderException}.
     *
     * @return a valid token
     */
    @SuppressWarnings({"InvalidThrows"})
    public synchronized Mono<String> getToken() {
        if (token == null || System.currentTimeMillis() > expiryTime) {
            return fetchAndUpdateToken();
        }
        return Mono.just(token);
    }

    private Mono<String> fetchAndUpdateToken() {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        return webClient.post()
                        .uri(uriBuilder -> uriBuilder.path("/oauth2/v3/token").build())
                        .headers(httpHeaders -> {
                            httpHeaders.setBasicAuth(configuration.clientId(), configuration.clientSecret());
                            httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                        })
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(TokenResponse.class)
                        .map(tokenResponse -> {
                            expiryTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(tokenResponse.expires_in());
                            token = tokenResponse.accessToken();
                            return token;
                        })
                        .onErrorMap(TokenProviderException::new);
    }
}
