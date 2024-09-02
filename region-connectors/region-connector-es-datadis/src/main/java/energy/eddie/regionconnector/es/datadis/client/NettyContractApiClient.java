package energy.eddie.regionconnector.es.datadis.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.regionconnector.es.datadis.api.ContractApi;
import energy.eddie.regionconnector.es.datadis.api.DatadisApiException;
import energy.eddie.regionconnector.es.datadis.config.DatadisConfig;
import energy.eddie.regionconnector.es.datadis.dtos.ContractDetails;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringEncoder;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Component
public class NettyContractApiClient implements ContractApi {

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final DatadisTokenProvider tokenProvider;
    private final URI suppliesEndpoint;

    public NettyContractApiClient(
            HttpClient httpClient,
            ObjectMapper mapper,
            DatadisTokenProvider tokenProvider,
            DatadisConfig config
    ) {
        this.httpClient = httpClient;
        this.mapper = mapper;
        this.tokenProvider = tokenProvider;
        this.suppliesEndpoint = URI.create(config.basePath()).resolve("api-private/api/get-contract-detail");
    }

    @Override
    public Mono<List<ContractDetails>> getContractDetails(String authorizedNif, String distributorCode, String cups) {
        QueryStringEncoder encoder = new QueryStringEncoder(suppliesEndpoint.toString());
        encoder.addParam("authorizedNif", authorizedNif);
        encoder.addParam("distributorCode", distributorCode);
        encoder.addParam("cups", cups);

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
                .responseSingle((httpClientResponse, byteBufMono) -> byteBufMono
                        .asString()
                        .defaultIfEmpty(Strings.EMPTY)
                        .flatMap(bodyString -> mapResponse(httpClientResponse, bodyString))
                ));
    }

    private Mono<List<ContractDetails>> mapResponse(HttpClientResponse httpClientResponse, String bodyString) {
        if (httpClientResponse.status().code() != HttpResponseStatus.OK.code()) {
            return Mono.error(new DatadisApiException(
                    "Failed to fetch contracts",
                    httpClientResponse.status(),
                    bodyString
            ));
        }
        try {
            List<ContractDetails> contracts = mapper.readValue(bodyString, new TypeReference<>() {});
            return Mono.just(contracts);
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }
    }
}
