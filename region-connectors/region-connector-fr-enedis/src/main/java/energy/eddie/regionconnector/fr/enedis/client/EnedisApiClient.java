package energy.eddie.regionconnector.fr.enedis.client;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.HealthState;
import energy.eddie.regionconnector.fr.enedis.api.EnedisApi;
import energy.eddie.regionconnector.fr.enedis.dto.EnedisContractApiResponse;
import energy.eddie.regionconnector.fr.enedis.dto.EnedisDataApiResponse;
import energy.eddie.regionconnector.fr.enedis.dto.MeterReading;
import energy.eddie.regionconnector.fr.enedis.dto.contract.CustomerContract;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class EnedisApiClient implements EnedisApi {

    public static final String USAGE_POINT_ID_PARAM = "usage_point_id";
    static final String AUTHENTICATION_API = "AuthenticationAPI";
    static final String METERING_POINT_API = "MeteringPointAPI";
    static final String CONTRACT_API = "ContractAPI";
    static final String CONTRACT_ENDPOINT = "customers_upc/v5/usage_points/contracts";
    private final EnedisTokenProvider tokenProvider;
    private final Map<String, HealthState> healthChecks = new HashMap<>();
    private final WebClient webClient;


    public EnedisApiClient(EnedisTokenProvider tokenProvider, WebClient webClient) {
        this.tokenProvider = tokenProvider;
        this.webClient = webClient;
        healthChecks.put(AUTHENTICATION_API, HealthState.UP);
        healthChecks.put(METERING_POINT_API, HealthState.UP);
        healthChecks.put(CONTRACT_API, HealthState.UP);
    }

    @Override
    public Mono<MeterReading> getConsumptionMeterReading(
            String usagePointId,
            LocalDate start,
            LocalDate end,
            Granularity granularity
    ) {
        return tokenProvider
                .getToken()
                .doOnSuccess(token -> healthChecks.put(AUTHENTICATION_API, HealthState.UP))
                .doOnError(throwable -> healthChecks.put(AUTHENTICATION_API, HealthState.DOWN))
                .flatMap(token -> webClient
                        .get()
                        .uri(uriBuilder -> uriBuilder
                                .path(granularityToPath(granularity))
                                .queryParam(USAGE_POINT_ID_PARAM, usagePointId)
                                .queryParam("start", start.format(DateTimeFormatter.ISO_DATE))
                                .queryParam("end", end.format(DateTimeFormatter.ISO_DATE))
                                .build())
                        .headers(headers -> headers.setBearerAuth(token))
                        .retrieve()
                        .bodyToMono(EnedisDataApiResponse.class)
                        .map(EnedisDataApiResponse::meterReading)
                        .doOnSuccess(o -> healthChecks.put(METERING_POINT_API, HealthState.UP))
                        .doOnError(throwable -> healthChecks.put(METERING_POINT_API, HealthState.DOWN))
                );
    }

    private String granularityToPath(Granularity granularity) {
        return switch (granularity) {
            case PT30M -> "metering_data_clc/v5/consumption_load_curve";
            case P1D -> "metering_data_dc/v5/daily_consumption";
            default -> throw new IllegalArgumentException("Unsupported granularity: " + granularity);
        };
    }

    @Override
    public Mono<CustomerContract> getContract(String usagePointId) {
        return tokenProvider
                .getToken()
                .doOnSuccess(token -> healthChecks.put(AUTHENTICATION_API, HealthState.UP))
                .doOnError(throwable -> healthChecks.put(AUTHENTICATION_API, HealthState.DOWN))
                .flatMap(token -> webClient
                        .get()
                        .uri(uriBuilder -> uriBuilder
                                .path(CONTRACT_ENDPOINT)
                                .queryParam(USAGE_POINT_ID_PARAM, usagePointId)
                                .build())
                        .headers(headers -> headers.setBearerAuth(token))
                        .retrieve()
                        .bodyToMono(EnedisContractApiResponse.class)
                        .map(EnedisContractApiResponse::customer)
                        .doOnSuccess(o -> healthChecks.put(CONTRACT_API, HealthState.UP))
                        .doOnError(throwable -> healthChecks.put(CONTRACT_API, HealthState.DOWN))
                );
    }

    @Override
    public Map<String, HealthState> health() {
        return healthChecks;
    }
}
