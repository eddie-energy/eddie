package energy.eddie.regionconnector.es.datadis.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.regionconnector.es.datadis.config.DatadisConfig;
import jakarta.annotation.Nullable;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

public class NettyDatadisTokenProvider implements DatadisTokenProvider {
    private final DatadisConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final DatadisEndpoints endpoints;
    @Nullable
    private String token;
    private long expiryTime;

    public NettyDatadisTokenProvider(DatadisConfig config, HttpClient httpClient, DatadisEndpoints endpoints) {
        requireNonNull(config);
        requireNonNull(httpClient);
        requireNonNull(endpoints);

        this.config = config;
        this.httpClient = httpClient;
        this.endpoints = endpoints;
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
                .uri(endpoints.tokenEndpoint())
                .sendForm((req, form) -> form.multipart(false)
                        .attr("username", config.username())
                        .attr("password", config.password())
                )
                .responseSingle((httpClientResponse, byteBufMono) -> {
                    if (httpClientResponse.status().code() != 200) {
                        return byteBufMono.asString().flatMap(bodyString -> Mono.error(new TokenProviderException("Failed to fetch token - " + bodyString)));
                    }
                    return byteBufMono.asString();
                });
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
        } catch (JsonProcessingException e) {
            return Mono.error(new TokenProviderException(e));
        }
    }

    public long getExpiryTime() {
        return expiryTime;
    }
}