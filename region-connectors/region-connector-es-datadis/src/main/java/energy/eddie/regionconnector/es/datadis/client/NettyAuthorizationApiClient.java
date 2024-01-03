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
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufMono;
import reactor.netty.http.client.HttpClient;

import static java.util.Objects.requireNonNull;

public class NettyAuthorizationApiClient implements AuthorizationApi {
    private final HttpClient httpClient;

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final DatadisTokenProvider tokenProvider;
    private final DatadisEndpoints endpoints;

    public NettyAuthorizationApiClient(HttpClient httpClient, DatadisTokenProvider tokenProvider, DatadisEndpoints endpoints) {
        requireNonNull(httpClient);
        requireNonNull(tokenProvider);

        this.httpClient = httpClient;
        this.tokenProvider = tokenProvider;
        this.endpoints = endpoints;
    }

    @Override
    public Mono<AuthorizationRequestResponse> postAuthorizationRequest(AuthorizationRequest authorizationRequest) {
        String body;

        try {
            body = mapper.writeValueAsString(authorizationRequest);
        } catch (JsonProcessingException e) {
            return Mono.error(new DatadisApiException(e));
        }

        return tokenProvider.getToken().flatMap(token -> httpClient
                .headers(headers -> headers.add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON))
                .headers(headers -> headers.add(HttpHeaderNames.AUTHORIZATION, "Bearer " + token))
                .post()
                .uri(endpoints.authorizationRequestEndpoint())
                .send(ByteBufMono.fromString(Mono.just(body)))
                .responseSingle((httpClientResponse, byteBufMono) -> byteBufMono.asString().flatMap(bodyString -> {
                    if (httpClientResponse.status().code() == HttpResponseStatus.OK.code()) {
                        try {
                            JsonNode jsonNode = mapper.readTree(bodyString);
                            return switch (jsonNode.get("response").asText()) {
                                case "ok" -> Mono.just(AuthorizationRequestResponse.OK);
                                case "nonif" -> Mono.just(AuthorizationRequestResponse.NO_NIF);
                                case "noSupplies" -> Mono.just(AuthorizationRequestResponse.NO_SUPPLIES);
                                default ->
                                        Mono.error(new DatadisApiException("Unexpected response: " + jsonNode.get("response").asText()));
                            };
                        } catch (JsonProcessingException e) {
                            return Mono.error(new DatadisApiException(e));
                        }
                    } else {
                        return Mono.error(new DatadisApiException("Failed to post authorization request: " + httpClientResponse.status().code() + " " + bodyString));
                    }
                }))
        );
    }
}
