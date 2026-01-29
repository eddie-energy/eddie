// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messenger;

import com.nimbusds.jwt.SignedJWT;
import energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapterConfiguration;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.text.ParseException;
import java.time.Instant;

@Component
public class WebClientPontonTokenProvider implements PontonTokenProvider {
    private final WebClient webClient;
    private final PontonXPAdapterConfiguration config;
    @Nullable
    private String token;

    private Instant expirationDate = Instant.now();


    public WebClientPontonTokenProvider(WebClient webClient, PontonXPAdapterConfiguration config) {
        this.webClient = webClient;
        this.config = config;
    }

    @Override
    public synchronized Mono<String> getToken() {
        if (token == null || Instant.now().isAfter(expirationDate)) {
            return fetchTokenFromServer()
                    .flatMap(this::validateAndUpdateToken);
        }
        return Mono.just(token);
    }

    private Mono<String> fetchTokenFromServer() {
        return webClient
                .post()
                .uri(config.apiEndpoint() + "/authenticate")
                .bodyValue(new AuthRequest(config.username(), config.password()))
                .retrieve()
                .bodyToMono(AuthResponse.class)
                .map(AuthResponse::token);
    }

    private synchronized Mono<String> validateAndUpdateToken(String jwt) {
        try {
            var signedJwt = SignedJWT.parse(jwt);
            expirationDate = signedJwt.getJWTClaimsSet().getExpirationTime().toInstant();
            token = jwt;
            return Mono.just(jwt);
        } catch (ParseException e) {
            return Mono.error(e);
        }
    }

    private record AuthResponse(
            String username,
            String setupTwoFactor,
            String token
    ) {
    }

    private record AuthRequest(
            String username,
            String password
    ) {
    }
}
