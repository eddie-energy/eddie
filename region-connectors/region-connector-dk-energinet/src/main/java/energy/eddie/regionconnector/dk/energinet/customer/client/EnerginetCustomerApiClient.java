package energy.eddie.regionconnector.dk.energinet.customer.client;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.api.v0.HealthState;
import energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnector;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.customer.api.IsAliveApi;
import energy.eddie.regionconnector.dk.energinet.customer.api.MeterDataApi;
import energy.eddie.regionconnector.dk.energinet.customer.api.TokenApi;
import energy.eddie.regionconnector.dk.energinet.customer.invoker.ApiClient;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointsRequest;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocumentResponseListApiResponse;
import energy.eddie.regionconnector.dk.energinet.customer.model.StringApiResponse;
import energy.eddie.regionconnector.dk.energinet.enums.TimeSeriesAggregationEnum;
import energy.eddie.regionconnector.dk.energinet.utils.ConsumptionRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

@Component
public class EnerginetCustomerApiClient implements EnerginetCustomerApi {
    private static final String IS_ALIVE_API = "isAliveApi";
    // Request period must not exceed the maximum number of days of 730
    private static final int MAX_REQUEST_PERIOD = 730;
    private final ApiClient apiClient;
    private final TokenApi tokenApi;
    private final MeterDataApi meterDataApi;
    private final IsAliveApi isAliveApi;

    @Autowired
    public EnerginetCustomerApiClient(EnerginetConfiguration configuration) {
        apiClient = new ApiClient()
                .setBasePath(configuration.customerBasePath());
        tokenApi = new TokenApi(apiClient);
        meterDataApi = new MeterDataApi(apiClient);
        isAliveApi = new IsAliveApi(apiClient);
    }

    public EnerginetCustomerApiClient(ApiClient apiClient, TokenApi tokenApi, MeterDataApi meterDataApi, IsAliveApi isAliveApi) {
        this.apiClient = apiClient;
        this.tokenApi = tokenApi;
        this.meterDataApi = meterDataApi;
        this.isAliveApi = isAliveApi;
    }

    private void throwIfInvalidTimeframe(ZonedDateTime start, ZonedDateTime end) throws DateTimeException {
        LocalDate currentDate = LocalDate.ofInstant(Instant.now(), EnerginetRegionConnector.DK_ZONE_ID);

        if (start.isEqual(end) || start.isAfter(end)) {
            throw new DateTimeException("Start date must be before end date.");
        }
        if (end.toLocalDate().isEqual(currentDate) || end.toLocalDate().isAfter(currentDate)) {
            throw new DateTimeException("The end date parameter must be earlier than the current date.");
        }
        if (start.plusDays(MAX_REQUEST_PERIOD).isBefore(end)) {
            throw new DateTimeException("Request period exceeds the maximum number of days (" + MAX_REQUEST_PERIOD + ").");
        }
    }

    @Override
    public Mono<Boolean> isAlive() {
        synchronized (apiClient) {
            return isAliveApi.apiIsaliveGet();
        }
    }

    @Override
    public Mono<String> accessToken(String refreshToken) {
        synchronized (apiClient) {
            setApiKey(refreshToken);
            return tokenApi.apiTokenGet()
                    .mapNotNull(StringApiResponse::getResult);
        }
    }

    @Override
    public Mono<ConsumptionRecord> getTimeSeries(
            ZonedDateTime dateFrom,
            ZonedDateTime dateTo,
            Granularity granularity,
            MeteringPointsRequest meteringPointsRequest,
            String accessToken,
            UUID userCorrelationId
    ) {
        throwIfInvalidTimeframe(dateFrom, dateTo);
        TimeSeriesAggregationEnum aggregation = TimeSeriesAggregationEnum.fromGranularity(granularity);
        synchronized (apiClient) {
            setApiKey(accessToken);
            return
                    meterDataApi.apiMeterdataGettimeseriesDateFromDateToAggregationPost(
                                    dateFrom.toLocalDate().toString(),
                                    dateTo.toLocalDate().toString(),
                                    aggregation.toString(),
                                    userCorrelationId,
                                    meteringPointsRequest
                            )
                            .mapNotNull(MyEnergyDataMarketDocumentResponseListApiResponse::getResult)
                            .map(ConsumptionRecordMapper::timeSeriesToCIM);
        }
    }

    private void setApiKey(String token) {
        apiClient.setApiKey("Bearer " + token);
    }

    @Override
    public Mono<Map<String, HealthState>> health() {
        return isAlive()
                .map(isAlive -> Map.of(
                                IS_ALIVE_API,
                                Boolean.TRUE.equals(isAlive)
                                        ? HealthState.UP
                                        : HealthState.DOWN
                        )
                );
    }
}