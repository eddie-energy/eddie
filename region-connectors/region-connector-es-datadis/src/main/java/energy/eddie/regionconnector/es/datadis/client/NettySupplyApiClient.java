// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.client;

import energy.eddie.regionconnector.es.datadis.api.DatadisApiException;
import energy.eddie.regionconnector.es.datadis.api.SupplyApi;
import energy.eddie.regionconnector.es.datadis.config.DatadisConfiguration;
import energy.eddie.regionconnector.es.datadis.dtos.Supply;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringEncoder;
import jakarta.annotation.Nullable;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Component
public class NettySupplyApiClient implements SupplyApi {

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final DatadisTokenProvider tokenProvider;
    private final URI suppliesEndpoint;

    public NettySupplyApiClient(
            HttpClient httpClient,
            ObjectMapper mapper,
            DatadisTokenProvider tokenProvider,
            DatadisConfiguration config
    ) {
        this.httpClient = httpClient;
        this.mapper = mapper;
        this.tokenProvider = tokenProvider;
        this.suppliesEndpoint = URI.create(config.basepath()).resolve("api-private/api/get-supplies");
    }

    @Override
    public Mono<List<Supply>> getSupplies(String authorizedNif, @Nullable String distributorCode) {
        QueryStringEncoder encoder = new QueryStringEncoder(suppliesEndpoint.toString());
        encoder.addParam("authorizedNif", authorizedNif);
        if (distributorCode != null) {
            encoder.addParam("distributorCode", distributorCode);
        }

        URI uri;
        try {
            uri = encoder.toUri();
        } catch (URISyntaxException e) {
            return Mono.error(e);
        }
        return tokenProvider.getToken().flatMap(token -> httpClient
                .headers(headers -> {
                    headers.add(HttpHeaderNames.AUTHORIZATION, "Bearer " + token);
                    headers.add(HttpHeaderNames.ACCEPT, "*/*");
                })
                .get()
                .uri(uri.toString())
                .responseSingle((httpClientResponse, byteBufMono) -> byteBufMono.asString()
                        .defaultIfEmpty(Strings.EMPTY)
                        .flatMap(bodyString -> {
                            if (httpClientResponse.status().code() != HttpResponseStatus.OK.code()) {
                                return Mono.error(new DatadisApiException("Failed to fetch supplies", httpClientResponse.status(), bodyString));
                            }
                            try {
                                List<Supply> supplies = mapper.readValue(bodyString, new TypeReference<>() {
                                });
                                return Mono.just(supplies);
                            } catch (JacksonException e) {
                                return Mono.error(e);
                            }
                        })));
    }
}