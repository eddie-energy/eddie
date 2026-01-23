// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.client;

import energy.eddie.regionconnector.es.datadis.config.DatadisConfiguration;
import jakarta.annotation.Nullable;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Component
public class NettyDatadisTokenProvider implements DatadisTokenProvider {
    private final DatadisConfiguration config;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final URI tokenEndpoint;
    @Nullable
    private String token;
    private long expiryTime;

    public NettyDatadisTokenProvider(
            DatadisConfiguration config,
            HttpClient httpClient,
            ObjectMapper mapper
    ) {
        this.config = config;
        this.httpClient = httpClient;
        this.tokenEndpoint = URI.create(config.basepath()).resolve("nikola-auth/tokens/login");
        this.mapper = mapper;
    }

    @Override
    public synchronized Mono<String> getToken() {
        if (token == null || TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) > expiryTime) {
            return fetchTokenFromServer()
                    .doOnNext(this::updateTokenAndExpiry);
        }
        return Mono.just(token);
    }

    private Mono<String> fetchTokenFromServer() {
        return httpClient
                .post()
                .uri(tokenEndpoint)
                .sendForm((req, form) -> form.multipart(false)
                                             .attr("username", config.username())
                                             .attr("password", config.password())
                                             .attr("origin", "WEB")
                )
                .responseSingle((httpClientResponse, byteBufMono) -> byteBufMono
                        .asString()
                        .defaultIfEmpty(Strings.EMPTY)
                        .flatMap(bodyString -> {
                            if (httpClientResponse.status().code() != 200 || bodyString.isEmpty()) {
                                return Mono.error(new TokenProviderException(
                                        "Failed to fetch token:" + httpClientResponse.status()
                                                                                     .code() + " - " + bodyString
                                ));
                            }
                            return Mono.just(bodyString);
                        }));
    }

    Mono<String> updateTokenAndExpiry(String jwtToken) {
        String[] splitToken = jwtToken.split("\\.", -1);
        if (splitToken.length != 3) {
            return Mono.error(new TokenProviderException("Invalid JWT token"));
        }
        String base64EncodedBody = splitToken[1];
        String body = new String(Base64.getDecoder().decode(base64EncodedBody), StandardCharsets.UTF_8);

        JsonNode jsonNode;
        try {
            jsonNode = mapper.readTree(body);
            token = jwtToken;
            expiryTime = jsonNode.get("exp").asLong();
            return Mono.just(token);
        } catch (JacksonException e) {
            return Mono.error(new TokenProviderException(e));
        }
    }

    public long getExpiryTime() {
        return expiryTime;
    }
}
