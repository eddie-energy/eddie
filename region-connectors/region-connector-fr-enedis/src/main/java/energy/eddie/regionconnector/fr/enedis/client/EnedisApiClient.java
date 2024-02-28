package energy.eddie.regionconnector.fr.enedis.client;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.HealthState;
import energy.eddie.regionconnector.fr.enedis.api.EnedisApi;
import energy.eddie.regionconnector.fr.enedis.dto.EnedisDataApiResponse;
import energy.eddie.regionconnector.fr.enedis.dto.MeterReading;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class EnedisApiClient implements EnedisApi {

    static final String AUTHENTICATION_API = "AuthenticationAPI";
    static final String METERING_POINT_API = "MeteringPointAPI";
    private final EnedisTokenProvider tokenProvider;
    private final Map<String, HealthState> healthChecks = new HashMap<>();
    private final WebClient webClient;


    public EnedisApiClient(EnedisTokenProvider tokenProvider, WebClient webClient) {
        this.tokenProvider = tokenProvider;
        this.webClient = webClient;
        healthChecks.put(AUTHENTICATION_API, HealthState.UP);
        healthChecks.put(METERING_POINT_API, HealthState.UP);
    }

    private String granularityToPath(Granularity granularity) {
        return switch (granularity) {
            case PT30M -> "metering_data_clc/v5/consumption_load_curve";
            case P1D -> "metering_data_dc/v5/daily_consumption";
            default -> throw new IllegalArgumentException("Unsupported granularity: " + granularity);
        };
    }

    @Override
    public Mono<MeterReading> getConsumptionMeterReading(String usagePointId, LocalDate start, LocalDate end, Granularity granularity) {
        return tokenProvider.getToken()
                .doOnSuccess(token -> healthChecks.put(AUTHENTICATION_API, HealthState.UP))
                .doOnError(throwable -> healthChecks.put(AUTHENTICATION_API, HealthState.DOWN))
                .flatMap(token ->
                        webClient
                                .get()
                                .uri(uriBuilder -> uriBuilder.path(granularityToPath(granularity))
                                        .queryParam("usage_point_id", usagePointId)
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

    @Override
    public Map<String, HealthState> health() {
        return healthChecks;
    }
}