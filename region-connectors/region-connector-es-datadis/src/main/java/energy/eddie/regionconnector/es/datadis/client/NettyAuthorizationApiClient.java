package energy.eddie.regionconnector.es.datadis.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.api.DatadisApiException;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequest;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequestResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.logging.log4j.util.Strings;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufMono;
import reactor.netty.http.client.HttpClient;

import java.net.URI;

import static java.util.Objects.requireNonNull;

public class NettyAuthorizationApiClient implements AuthorizationApi {
    private final HttpClient httpClient;

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final DatadisTokenProvider tokenProvider;
    private final URI authorizationEndpoint;

    public NettyAuthorizationApiClient(HttpClient httpClient, DatadisTokenProvider tokenProvider, String basePath) {
        requireNonNull(httpClient);
        requireNonNull(tokenProvider);
        requireNonNull(basePath);

        this.httpClient = httpClient;
        this.tokenProvider = tokenProvider;
        this.authorizationEndpoint = URI.create(basePath).resolve("api-private/request/send-request-authorization");
    }

    @Override
    public Mono<AuthorizationRequestResponse> postAuthorizationRequest(AuthorizationRequest authorizationRequest) {
        String body;

        try {
            body = mapper.writeValueAsString(authorizationRequest);
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }

        return tokenProvider.getToken().flatMap(token -> httpClient
                .headers(headers -> headers.add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON))
                .headers(headers -> headers.add(HttpHeaderNames.AUTHORIZATION, "Bearer " + token))
                .post()
                .uri(authorizationEndpoint)
                .send(ByteBufMono.fromString(Mono.just(body)))
                .responseSingle((httpClientResponse, byteBufMono) -> byteBufMono.asString()
                        .defaultIfEmpty(Strings.EMPTY)
                        .flatMap(bodyString -> {
                            if (httpClientResponse.status().code() == HttpResponseStatus.OK.code()) {
                                try {
                                    JsonNode jsonNode = mapper.readTree(bodyString);
                                    return Mono.just(AuthorizationRequestResponse.fromResponse(jsonNode.get("response").asText()));
                                } catch (JsonProcessingException e) {
                                    return Mono.error(e);
                                }
                            } else {
                                return Mono.error(new DatadisApiException("Failed to post authorization request", httpClientResponse.status(), bodyString));
                            }
                        }))
        );
    }
}