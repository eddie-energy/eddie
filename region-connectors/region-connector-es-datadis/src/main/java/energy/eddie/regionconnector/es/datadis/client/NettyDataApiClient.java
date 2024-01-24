package energy.eddie.regionconnector.es.datadis.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.regionconnector.es.datadis.api.DataApi;
import energy.eddie.regionconnector.es.datadis.api.DatadisApiException;
import energy.eddie.regionconnector.es.datadis.dtos.MeteringData;
import energy.eddie.regionconnector.es.datadis.dtos.MeteringDataRequest;
import energy.eddie.regionconnector.es.datadis.dtos.Supply;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringEncoder;
import jakarta.annotation.Nullable;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.util.Objects.requireNonNull;

public class NettyDataApiClient implements DataApi {

    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM");
    private final DatadisTokenProvider tokenProvider;
    private final DatadisEndpoints endpoints;

    public NettyDataApiClient(HttpClient httpClient, DatadisTokenProvider tokenProvider, DatadisEndpoints endpoints) {
        requireNonNull(httpClient);
        requireNonNull(tokenProvider);

        this.httpClient = httpClient;
        this.tokenProvider = tokenProvider;
        this.endpoints = endpoints;
    }

    @Override
    public Mono<List<Supply>> getSupplies(String authorizedNif, @Nullable String distributorCode) {
        QueryStringEncoder encoder = new QueryStringEncoder(endpoints.suppliesEndpoint().toString());
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
                .headers(headers -> headers.add(HttpHeaderNames.AUTHORIZATION, "Bearer " + token))
                .get()
                .uri(uri.toString())
                .responseSingle((httpClientResponse, byteBufMono) -> byteBufMono.asString().flatMap(bodyString -> {
                    if (httpClientResponse.status().code() != HttpResponseStatus.OK.code()) {
                        return Mono.error(new DatadisApiException("Failed to fetch supplies", httpClientResponse.status(), bodyString));
                    }
                    try {
                        List<Supply> supplies = mapper.readValue(bodyString, new TypeReference<>() {
                        });
                        return Mono.just(supplies);
                    } catch (JsonProcessingException e) {
                        return Mono.error(e);
                    }
                })));
    }

    @Override
    public Mono<List<MeteringData>> getConsumptionKwh(MeteringDataRequest meteringDataRequest) {
        QueryStringEncoder encoder = new QueryStringEncoder(endpoints.consumptionKwhEndpoint().toString());
        encoder.addParam("authorizedNif", meteringDataRequest.authorizedNif());
        encoder.addParam("cups", meteringDataRequest.meteringPoint());
        encoder.addParam("distributorCode", meteringDataRequest.distributorCode());
        encoder.addParam("startDate", meteringDataRequest.startDate().format(formatter));
        encoder.addParam("endDate", meteringDataRequest.endDate().format(formatter));
        encoder.addParam("measurementType", String.valueOf(meteringDataRequest.measurementType().getValue()));
        encoder.addParam("pointType", meteringDataRequest.pointType());

        URI uri;

        try {
            uri = encoder.toUri();
        } catch (URISyntaxException e) {
            return Mono.error(e);
        }
        return tokenProvider.getToken().flatMap(token -> httpClient
                .headers(headers -> headers.add(HttpHeaderNames.AUTHORIZATION, "Bearer " + token))
                .get()
                .uri(uri)
                .responseSingle((httpClientResponse, byteBufMono) -> byteBufMono.asString().flatMap(body -> {
                    if (httpClientResponse.status().code() != HttpResponseStatus.OK.code()) {
                        return Mono.error(new DatadisApiException("Failed to fetch consumptionKwh", httpClientResponse.status(), body));
                    }

                    try {
                        List<MeteringData> meteringData = mapper.readValue(body, new TypeReference<>() {
                        });
                        return Mono.just(meteringData);
                    } catch (JsonProcessingException e) {
                        return Mono.error(e);
                    }
                })));
    }
}