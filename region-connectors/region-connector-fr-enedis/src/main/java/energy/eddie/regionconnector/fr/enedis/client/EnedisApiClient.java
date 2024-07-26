package energy.eddie.regionconnector.fr.enedis.client;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.HealthState;
import energy.eddie.regionconnector.fr.enedis.api.EnedisApi;
import energy.eddie.regionconnector.fr.enedis.dto.EnedisContractApiResponse;
import energy.eddie.regionconnector.fr.enedis.dto.EnedisDataApiResponse;
import energy.eddie.regionconnector.fr.enedis.dto.MeterReading;
import energy.eddie.regionconnector.fr.enedis.dto.contract.CustomerContract;
import energy.eddie.regionconnector.fr.enedis.providers.MeterReadingType;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnedisApiClient implements EnedisApi {

    public static final String USAGE_POINT_ID_PARAM = "usage_point_id";
    public static final String METERING_DATA_CLC_V_5_CONSUMPTION_LOAD_CURVE = "metering_data_clc/v5/consumption_load_curve";
    public static final String METERING_DATA_DC_V_5_DAILY_CONSUMPTION = "metering_data_dc/v5/daily_consumption";
    public static final String METERING_DATA_PLC_V_5_PRODUCTION_LOAD_CURVE = "metering_data_plc/v5/production_load_curve";
    public static final String METERING_DATA_DP_V_5_DAILY_PRODUCTION = "metering_data_dp/v5/daily_production";
    public static final String AUTHENTICATION_API = "AuthenticationAPI";
    public static final String METERING_POINT_API = "MeteringPointAPI";
    public static final String CONTRACT_API = "ContractAPI";
    private static final String CONTRACT_ENDPOINT = "customers_upc/v5/usage_points/contracts";
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
        return getMeterReading(usagePointId, start, end, granularity, MeterReadingType.CONSUMPTION);
    }

    @Override
    public Mono<MeterReading> getProductionMeterReading(
            String usagePointId,
            LocalDate start,
            LocalDate end,
            Granularity granularity
    ) {
        return getMeterReading(usagePointId, start, end, granularity, MeterReadingType.PRODUCTION);
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
                        .headers(headers -> headers.setAccept(List.of(MediaType.APPLICATION_JSON)))
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

    private Mono<MeterReading> getMeterReading(
            String usagePointId,
            LocalDate start,
            LocalDate end,
            Granularity granularity,
            MeterReadingType type
    ) {
        return tokenProvider
                .getToken()
                .doOnSuccess(token -> healthChecks.put(AUTHENTICATION_API, HealthState.UP))
                .doOnError(throwable -> healthChecks.put(AUTHENTICATION_API, HealthState.DOWN))
                .flatMap(token -> webClient
                        .get()
                        .uri(uriBuilder -> uriBuilder
                                .path(granularityToPath(granularity, type))
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

    private String granularityToPath(Granularity granularity, MeterReadingType type) {
        return switch (type) {
            case CONSUMPTION -> switch (granularity) {
                case PT30M -> METERING_DATA_CLC_V_5_CONSUMPTION_LOAD_CURVE;
                case P1D -> METERING_DATA_DC_V_5_DAILY_CONSUMPTION;
                default -> throw new IllegalArgumentException("Unsupported granularity: " + granularity);
            };
            case PRODUCTION -> switch (granularity) {
                case PT30M -> METERING_DATA_PLC_V_5_PRODUCTION_LOAD_CURVE;
                case P1D -> METERING_DATA_DP_V_5_DAILY_PRODUCTION;
                default -> throw new IllegalArgumentException("Unsupported granularity: " + granularity);
            };
        };
    }
}
