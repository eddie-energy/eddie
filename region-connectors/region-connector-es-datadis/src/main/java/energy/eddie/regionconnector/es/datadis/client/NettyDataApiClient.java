package energy.eddie.regionconnector.es.datadis.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.regionconnector.es.datadis.api.DataApi;
import energy.eddie.regionconnector.es.datadis.api.DatadisApiException;
import energy.eddie.regionconnector.es.datadis.dtos.MeteringData;
import energy.eddie.regionconnector.es.datadis.dtos.MeteringDataRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringEncoder;
import org.apache.logging.log4j.util.Strings;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.util.Objects.requireNonNull;

public class NettyDataApiClient implements DataApi {

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM");
    private final DatadisTokenProvider tokenProvider;
    private final URI consumptionKwhEndpoint;

    public NettyDataApiClient(HttpClient httpClient, ObjectMapper mapper, DatadisTokenProvider tokenProvider, String basePath) {
        requireNonNull(httpClient);
        requireNonNull(mapper);
        requireNonNull(tokenProvider);
        requireNonNull(basePath);

        this.httpClient = httpClient;
        this.mapper = mapper;
        this.tokenProvider = tokenProvider;
        this.consumptionKwhEndpoint = URI.create(basePath).resolve("api-private/api/get-consumption-data");
    }

    @Override
    public Mono<List<MeteringData>> getConsumptionKwh(MeteringDataRequest meteringDataRequest) {
        QueryStringEncoder encoder = new QueryStringEncoder(consumptionKwhEndpoint.toString());
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
                .responseSingle((httpClientResponse, byteBufMono) -> byteBufMono.asString()
                        .defaultIfEmpty(Strings.EMPTY)
                        .flatMap(body -> {
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