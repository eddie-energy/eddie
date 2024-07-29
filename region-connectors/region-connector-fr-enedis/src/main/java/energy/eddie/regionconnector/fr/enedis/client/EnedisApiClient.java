package energy.eddie.regionconnector.fr.enedis.client;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.HealthState;
import energy.eddie.regionconnector.fr.enedis.api.EnedisAccountingPointDataApi;
import energy.eddie.regionconnector.fr.enedis.api.EnedisHealth;
import energy.eddie.regionconnector.fr.enedis.api.EnedisMeterReadingApi;
import energy.eddie.regionconnector.fr.enedis.dto.EnedisContractApiResponse;
import energy.eddie.regionconnector.fr.enedis.dto.EnedisDataApiResponse;
import energy.eddie.regionconnector.fr.enedis.dto.MeterReading;
import energy.eddie.regionconnector.fr.enedis.dto.address.CustomerAddress;
import energy.eddie.regionconnector.fr.enedis.dto.contact.CustomerContact;
import energy.eddie.regionconnector.fr.enedis.dto.contract.CustomerContract;
import energy.eddie.regionconnector.fr.enedis.dto.identity.CustomerIdentity;
import energy.eddie.regionconnector.fr.enedis.providers.MeterReadingType;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class EnedisApiClient implements EnedisMeterReadingApi, EnedisAccountingPointDataApi, EnedisHealth {

    public static final String USAGE_POINT_ID_PARAM = "usage_point_id";
    public static final String METERING_DATA_CLC_V_5_CONSUMPTION_LOAD_CURVE = "metering_data_clc/v5/consumption_load_curve";
    public static final String METERING_DATA_DC_V_5_DAILY_CONSUMPTION = "metering_data_dc/v5/daily_consumption";
    public static final String METERING_DATA_PLC_V_5_PRODUCTION_LOAD_CURVE = "metering_data_plc/v5/production_load_curve";
    public static final String METERING_DATA_DP_V_5_DAILY_PRODUCTION = "metering_data_dp/v5/daily_production";
    public static final String AUTHENTICATION_API = "AuthenticationAPI";
    public static final String METERING_POINT_API = "MeteringPointAPI";
    public static final String CONTRACT_API = "ContractAPI";
    public static final String CONTACT_API = "ContactAPI";
    public static final String IDENTITY_API = "IdentityAPI";
    public static final String ADDRESS_API = "AddressAPI";
    private static final String CONTRACT_ENDPOINT = "customers_upc/v5/usage_points/contracts";
    private static final String CONTACT_ENDPOINT = "customers_upc/v5/usage_points/contact_data";
    private static final String IDENTITY_ENDPOINT = "customers_upc/v5/usage_points/identity";
    private static final String ADDRESS_ENDPOINT = "customers_upc/v5/usage_points/addresses";
    private final EnedisTokenProvider tokenProvider;
    private final Map<String, HealthState> healthChecks = new HashMap<>();
    private final WebClient webClient;


    public EnedisApiClient(EnedisTokenProvider tokenProvider, WebClient webClient) {
        this.tokenProvider = tokenProvider;
        this.webClient = webClient;
        healthChecks.put(AUTHENTICATION_API, HealthState.UP);
        healthChecks.put(METERING_POINT_API, HealthState.UP);
        healthChecks.put(CONTRACT_API, HealthState.UP);
        healthChecks.put(CONTACT_API, HealthState.UP);
        healthChecks.put(IDENTITY_API, HealthState.UP);
        healthChecks.put(ADDRESS_API, HealthState.UP);
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

    private Mono<MeterReading> getMeterReading(
            String usagePointId,
            LocalDate start,
            LocalDate end,
            Granularity granularity,
            MeterReadingType type
    ) {
        return getFromUri(
                uriBuilder -> uriBuilder
                        .path(granularityToPath(granularity, type))
                        .queryParam(USAGE_POINT_ID_PARAM, usagePointId)
                        .queryParam("start", start.format(DateTimeFormatter.ISO_DATE))
                        .queryParam("end", end.format(DateTimeFormatter.ISO_DATE))
                        .build(),
                METERING_POINT_API,
                EnedisDataApiResponse.class
        ).map(EnedisDataApiResponse::meterReading);
    }

    private <T> Mono<T> getFromUri(
            Function<UriBuilder, URI> uriFunction, String healthCheckKey, Class<T> responseType
    ) {
        return token()
                .flatMap(token -> webClient
                        .get()
                        .uri(uriFunction)
                        .headers(headers -> headers.setBearerAuth(token))
                        .headers(headers -> headers.setAccept(List.of(MediaType.APPLICATION_JSON)))
                        .retrieve()
                        .bodyToMono(responseType)
                        .doOnSuccess(o -> healthChecks.put(healthCheckKey, HealthState.UP))
                        .doOnError(throwable -> healthChecks.put(healthCheckKey, HealthState.DOWN))
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

    private @NotNull Mono<String> token() {
        return tokenProvider
                .getToken()
                .doOnSuccess(token -> healthChecks.put(AUTHENTICATION_API, HealthState.UP))
                .doOnError(throwable -> healthChecks.put(AUTHENTICATION_API, HealthState.DOWN));
    }

    @Override
    public Mono<CustomerContract> getContract(String usagePointId) {
        return getFromUri(
                uriBuilder -> uriBuilder.path(CONTRACT_ENDPOINT).queryParam(USAGE_POINT_ID_PARAM, usagePointId).build(),
                CONTRACT_API,
                EnedisContractApiResponse.class
        ).map(EnedisContractApiResponse::customer);
    }

    @Override
    public Mono<CustomerAddress> getAddress(String usagePointId) {
        return getFromUri(
                uriBuilder -> uriBuilder.path(ADDRESS_ENDPOINT).queryParam(USAGE_POINT_ID_PARAM, usagePointId).build(),
                ADDRESS_API,
                CustomerAddress.class
        );
    }

    @Override
    public Mono<CustomerIdentity> getIdentity(String usagePointId) {
        return getFromUri(
                uriBuilder -> uriBuilder.path(IDENTITY_ENDPOINT).queryParam(USAGE_POINT_ID_PARAM, usagePointId).build(),
                IDENTITY_API,
                CustomerIdentity.class
        );
    }

    @Override
    public Mono<CustomerContact> getContact(String usagePointId) {
        return getFromUri(
                uriBuilder -> uriBuilder.path(CONTACT_ENDPOINT).queryParam(USAGE_POINT_ID_PARAM, usagePointId).build(),
                CONTACT_API,
                CustomerContact.class
        );
    }

    @Override
    public Map<String, HealthState> health() {
        return healthChecks;
    }
}
