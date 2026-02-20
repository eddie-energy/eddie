// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.de.eta.oauth;

import energy.eddie.regionconnector.de.eta.config.DeEtaPlusConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Service
public class EtaOAuthService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EtaOAuthService.class);

    private final WebClient webClient;
    private final DeEtaPlusConfiguration configuration;

    public EtaOAuthService(WebClient.Builder webClientBuilder, DeEtaPlusConfiguration configuration) {
        this.webClient = webClientBuilder.build();
        this.configuration = configuration;
    }

    public Mono<OAuthTokenResponse> exchangeCodeForToken(String code, String openid) {
        LOGGER.info("Exchanging authorization token for access token");

        String tokenUrl = configuration.oauth().tokenUrl();

        String uri = UriComponentsBuilder.fromUriString(tokenUrl)
                .queryParam("token", code)
                .queryParam("openid", openid)
                .toUriString();

        return webClient.put()
                .uri(uri)
                .retrieve()
                .bodyToMono(OAuthTokenResponse.class)
                .doOnSuccess(response -> {
                    if (response != null && response.success()) {
                        LOGGER.info("Successfully exchanged token for access token");
                    } else {
                        LOGGER.warn("Token exchange returned unsuccessful response");
                    }
                })
                .doOnError(error -> LOGGER.error("Error during token exchange", error));
    }
}
