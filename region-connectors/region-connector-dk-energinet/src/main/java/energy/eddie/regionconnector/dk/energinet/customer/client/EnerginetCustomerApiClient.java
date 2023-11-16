package energy.eddie.regionconnector.dk.energinet.customer.client;

import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.api.v0.HealthState;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.customer.api.IsAliveApi;
import energy.eddie.regionconnector.dk.energinet.customer.api.MeterDataApi;
import energy.eddie.regionconnector.dk.energinet.customer.api.TokenApi;
import energy.eddie.regionconnector.dk.energinet.customer.invoker.ApiClient;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointsRequest;
import energy.eddie.regionconnector.dk.energinet.enums.PeriodResolutionEnum;
import energy.eddie.regionconnector.dk.energinet.enums.TimeSeriesAggregationEnum;
import energy.eddie.regionconnector.dk.energinet.utils.ConsumptionRecordMapper;

import java.time.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class EnerginetCustomerApiClient implements EnerginetCustomerApi {
    private static final String IS_ALIVE_API = "isAliveApi";
    // Request period must not exceed the maximum number of days of 730
    private static final int MAX_REQUEST_PERIOD = 730;
    private static final String DK_ZONE_ID = "Europe/Copenhagen";
    private final ApiClient apiClient;
    private final TokenApi tokenApi;
    private final MeterDataApi meterDataApi;
    private final IsAliveApi isAliveApi;
    private final Map<String, HealthState> healthChecks = new HashMap<>();
    private UUID userCorrelationId = UUID.randomUUID();
    private String refreshToken = "";
    private String accessToken = "";

    public EnerginetCustomerApiClient(EnerginetConfiguration propertiesEnerginetConfiguration) {
        apiClient = new ApiClient("Bearer");
        apiClient.setBasePath(propertiesEnerginetConfiguration.customerBasePath());

        tokenApi = apiClient.buildClient(TokenApi.class);
        meterDataApi = apiClient.buildClient(MeterDataApi.class);
        isAliveApi = apiClient.buildClient(IsAliveApi.class);
    }

    private void throwIfInvalidTimeframe(ZonedDateTime start, ZonedDateTime end) throws DateTimeException {
        LocalDate currentDate = LocalDate.ofInstant(Instant.now(), ZoneId.of(DK_ZONE_ID));

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
    public Boolean isAlive() {
        return isAliveApi.apiIsaliveGet();
    }

    @Override
    public void apiToken() {
        if (refreshToken.isBlank()) {
            throw new IllegalStateException("Refresh Token was not set");
        }

        setApiKey(refreshToken);
        accessToken = tokenApi.apiTokenGet().getResult();
    }

    @Override
    public ConsumptionRecord getTimeSeries(ZonedDateTime dateFrom,
                                           ZonedDateTime dateTo,
                                           PeriodResolutionEnum periodResolutionEnum,
                                           MeteringPointsRequest meteringPointsRequest) {
        throwIfInvalidTimeframe(dateFrom, dateTo);
        TimeSeriesAggregationEnum aggregation = TimeSeriesAggregationEnum.fromPointQualityEnum(periodResolutionEnum);

        if (accessToken.isBlank()) {
            apiToken();
        }

        setApiKey(accessToken);

        return ConsumptionRecordMapper.timeSeriesToCIM(
                Objects.requireNonNull(
                        meterDataApi.apiMeterdataGettimeseriesDateFromDateToAggregationPost(
                                dateFrom.toLocalDate().toString(),
                                dateTo.toLocalDate().toString(),
                                aggregation.toString(),
                                userCorrelationId,
                                meteringPointsRequest
                        ).getResult()
                )
        );
    }

    @Override
    public void setUserCorrelationId(UUID userCorrelationId) {
        this.userCorrelationId = userCorrelationId;
    }

    @Override
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = Objects.requireNonNull(refreshToken);
    }

    private void setApiKey(String token) {
        apiClient.setApiKey("Bearer " + token);
    }

    @Override
    public Map<String, HealthState> health() {
        if (Boolean.TRUE.equals(isAlive())) {
            healthChecks.put(IS_ALIVE_API, HealthState.UP);
        } else {
            healthChecks.put(IS_ALIVE_API, HealthState.DOWN);
        }
        return healthChecks;
    }
}
