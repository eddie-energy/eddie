// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.client;

import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.api.DatadisApiException;
import energy.eddie.regionconnector.es.datadis.config.DatadisConfiguration;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequest;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequestResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufMono;
import reactor.netty.http.client.HttpClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;

@Component
public class NettyAuthorizationApiClient implements AuthorizationApi {
    private final HttpClient httpClient;

    private final ObjectMapper mapper;
    private final DatadisTokenProvider tokenProvider;
    private final URI authorizationEndpoint;

    public NettyAuthorizationApiClient(
            HttpClient httpClient,
            ObjectMapper mapper,
            DatadisTokenProvider tokenProvider,
            DatadisConfiguration datadisConfig
    ) {
        this.httpClient = httpClient;
        this.mapper = mapper;
        this.tokenProvider = tokenProvider;
        this.authorizationEndpoint = URI.create(datadisConfig.basepath())
                                        .resolve("api-private/request/send-request-authorization");
    }

    @Override
    public Mono<AuthorizationRequestResponse> postAuthorizationRequest(AuthorizationRequest authorizationRequest) {
        String body;

        try {
            body = mapper.writeValueAsString(authorizationRequest);
        } catch (JacksonException e) {
            return Mono.error(e);
        }

        return tokenProvider
                .getToken()
                .flatMap(token -> httpClient
                        .headers(headers -> headers.add(HttpHeaderNames.CONTENT_TYPE,
                                                        HttpHeaderValues.APPLICATION_JSON))
                        .headers(headers -> headers.add(HttpHeaderNames.AUTHORIZATION, "Bearer " + token))
                        // Datadis blocks the spring user-agent, see GH-1102
                        .headers(headers -> headers.add(HttpHeaderNames.USER_AGENT, "PostmanRuntime/7.36.3"))
                        .post()
                        .uri(authorizationEndpoint)
                        .send(ByteBufMono.fromString(Mono.just(body)))
                        .responseSingle((httpClientResponse, byteBufMono) -> byteBufMono
                                .asString()
                                .defaultIfEmpty(Strings.EMPTY)
                                .flatMap(bodyString -> {
                                    if (httpClientResponse.status().code() == HttpResponseStatus.OK.code()) {
                                        try {
                                            JsonNode jsonNode = mapper.readTree(bodyString);
                                            return Mono.just(AuthorizationRequestResponse.fromResponse(
                                                    jsonNode.get("response").asString()
                                            ));
                                        } catch (JacksonException e) {
                                            return Mono.error(e);
                                        }
                                    } else {
                                        return Mono.error(new DatadisApiException(
                                                "Failed to post authorization request",
                                                httpClientResponse.status(),
                                                bodyString
                                        ));
                                    }
                                }))
                );
    }
}
