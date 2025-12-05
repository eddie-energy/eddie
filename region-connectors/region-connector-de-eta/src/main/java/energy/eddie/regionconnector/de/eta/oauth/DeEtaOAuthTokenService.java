package energy.eddie.regionconnector.de.eta.oauth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.regionconnector.de.eta.oauth.crypto.TokenCryptoService;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Handles exchanging authorization codes for tokens and refreshing tokens.
 */
@Service
public class DeEtaOAuthTokenService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeEtaOAuthTokenService.class);
    private static final String TAG_KEY_REGION = "region";
    private static final String REGION_DE_ETA = "de-eta";
    private static final Tags METRIC_TAGS = Tags.of(TAG_KEY_REGION, REGION_DE_ETA);

    private final WebClient webClient;
    private final DeEtaOAuthProperties props;
    private final DeEtaOAuthStateStore stateStore;
    private final DeEtaOAuthTokenRepository tokenRepository;
    private final TokenCryptoService crypto;
    private final Clock clock;

    public DeEtaOAuthTokenService(
            WebClient webClient,
            DeEtaOAuthProperties props,
            DeEtaOAuthStateStore stateStore,
            DeEtaOAuthTokenRepository tokenRepository,
            TokenCryptoService crypto
    ) {
        this(webClient, props, stateStore, tokenRepository, crypto, Clock.systemUTC());
    }

    @Autowired
    public DeEtaOAuthTokenService(
            WebClient webClient,
            DeEtaOAuthProperties props,
            DeEtaOAuthStateStore stateStore,
            DeEtaOAuthTokenRepository tokenRepository,
            TokenCryptoService crypto,
            Clock clock
    ) {
        this.webClient = webClient;
        this.props = props;
        this.stateStore = stateStore;
        this.tokenRepository = tokenRepository;
        this.crypto = crypto;
        this.clock = clock;
    }

    public Result exchangeAuthorizationCode(String code, String state) {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(state, "state");

        var stateOpt = stateStore.find(state);
        if (stateOpt.isEmpty()) {
            throw new IllegalStateException("Invalid or expired state");
        }
        var st = stateOpt.get();

        // Exchange code for tokens
        LOGGER.info("Starting token exchange for connectionId={} permissionId={}", st.connectionId(), st.permissionId());
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", props.redirectUri());
        form.add("client_id", props.clientId());
        // Never log clientSecret; just add to form
        form.add("client_secret", props.clientSecret());

        TokenResponse resp = webClient.post()
                .uri(props.tokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(form)
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .onErrorResume(ex -> {
                    // Never log tokens, just class info
                    LOGGER.warn("Token exchange failed: {}", ex.getClass().getSimpleName());
                    Metrics.counter("oauth.token.exchange.failure", METRIC_TAGS).increment();
                    return Mono.error(new DeEtaOAuthException("Token exchange failed", ex));
                })
                .block();

        if (resp == null || resp.accessToken == null || resp.accessToken.isBlank()) {
            Metrics.counter("oauth.token.exchange.failure", METRIC_TAGS).increment();
            throw new DeEtaOAuthException("Empty token response");
        }

        int expiresIn = resp.expiresIn <= 0 ? 3600 : resp.expiresIn;
        LocalDateTime expiresAt = LocalDateTime.ofInstant(clock.instant(), java.time.ZoneOffset.UTC)
                .plusSeconds(expiresIn);

        String encAccess = crypto.encrypt(resp.accessToken);
        String encRefresh = crypto.encrypt(resp.refreshToken);

        var entity = tokenRepository.findByConnectionId(st.connectionId())
                .map(existing -> {
                    existing.setTokens(encAccess, encRefresh, expiresAt, resp.scope);
                    return existing;
                })
                .orElseGet(() -> new DeEtaOAuthToken(st.connectionId(), encAccess, encRefresh, expiresAt, resp.scope));

        var saved = tokenRepository.save(entity);
        Metrics.counter("oauth.token.exchange.success", METRIC_TAGS).increment();
        LOGGER.info("Token exchange succeeded for connectionId={} permissionId={} tokenExpiresAt={}",
                st.connectionId(), st.permissionId(), expiresAt);
        return new Result(saved, st.permissionId().toString());
    }

    public String getValidAccessToken(String connectionId) {
        var token = tokenRepository.findByConnectionId(connectionId)
                .orElseThrow(() -> new IllegalStateException("Token not found for connection"));
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), java.time.ZoneOffset.UTC);
        if (token.getExpiresAt() == null || !token.getExpiresAt().isAfter(now.plus(Duration.ofSeconds(30)))) {
            // refresh
            LOGGER.info("Refreshing access token for connectionId={} tokenExpiresAt={}", connectionId, token.getExpiresAt());
            var rt = crypto.decrypt(token.getRefreshToken());
            if (rt == null || rt.isBlank()) {
                throw new IllegalStateException("No refresh token available");
            }
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "refresh_token");
            form.add("refresh_token", rt);
            form.add("client_id", props.clientId());
            form.add("client_secret", props.clientSecret());

            TokenResponse resp = webClient.post()
                    .uri(props.tokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(form)
                    .retrieve()
                    .bodyToMono(TokenResponse.class)
                    .onErrorResume(ex -> {
                        LOGGER.warn("Token refresh failed for connectionId={} reason={}", connectionId, ex.getClass().getSimpleName());
                        Metrics.counter("oauth.token.refresh.failure", METRIC_TAGS).increment();
                        return Mono.error(ex);
                    })
                    .block();

            if (resp == null || resp.accessToken == null || resp.accessToken.isBlank()) {
                Metrics.counter("oauth.token.refresh.failure", METRIC_TAGS).increment();
                throw new DeEtaOAuthException("Empty refresh response");
            }
            int expiresIn = resp.expiresIn <= 0 ? 3600 : resp.expiresIn;
            LocalDateTime expiresAt = LocalDateTime.ofInstant(clock.instant(), java.time.ZoneOffset.UTC)
                    .plusSeconds(expiresIn);
            String encAccess = crypto.encrypt(resp.accessToken);
            String encRefresh = crypto.encrypt(resp.refreshToken != null && !resp.refreshToken.isBlank() ? resp.refreshToken : rt);

            token.setTokens(encAccess, encRefresh, expiresAt, resp.scope);
            tokenRepository.save(token);
            Metrics.counter("oauth.token.refresh.success", METRIC_TAGS).increment();
            LOGGER.info("Token refreshed for connectionId={} tokenExpiresAt={}", connectionId, expiresAt);
        }
        return crypto.decrypt(token.getAccessToken());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TokenResponse {
        @JsonProperty("access_token")
        public String accessToken;
        @JsonProperty("refresh_token")
        public String refreshToken;
        @JsonProperty("expires_in")
        public int expiresIn;
        @JsonProperty("scope")
        public String scope;
        @JsonProperty("token_type")
        private String tokenType;

        public String getTokenType() { return tokenType; }
    }

    public record Result(DeEtaOAuthToken token, String permissionId) {}
}
