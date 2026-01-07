package energy.eddie.regionconnector.dk.energinet.customer.client;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.api.*;
import energy.eddie.regionconnector.dk.energinet.customer.invoker.ApiClient;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointDetailsCustomerDtoResponseListApiResponse;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointsRequest;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocumentResponseListApiResponse;
import energy.eddie.regionconnector.dk.energinet.customer.model.StringApiResponse;
import energy.eddie.regionconnector.dk.energinet.enums.TimeSeriesAggregationEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata.DK_ZONE_ID;

@Component
public class EnerginetCustomerApiClient implements EnerginetCustomerApi {
    // The Request period must not exceed the maximum number of days of 730
    private static final int MAX_REQUEST_PERIOD = 730;
    private final ApiClient apiClient;
    private final TokenApi tokenApi;
    private final MeterDataApi meterDataApi;
    private final MeteringPointsApi meteringPointsApi;
    private final IsAliveApi isAliveApi;

    @Autowired
    public EnerginetCustomerApiClient(EnerginetConfiguration configuration, WebClient webClient) {
        apiClient = new ApiClient(webClient)
                .setBasePath(configuration.customerBasePath());
        tokenApi = new TokenApi(apiClient);
        meterDataApi = new MeterDataApi(apiClient);
        isAliveApi = new IsAliveApi(apiClient);
        meteringPointsApi = new MeteringPointsApi(apiClient);
    }

    EnerginetCustomerApiClient(
            ApiClient apiClient,
            TokenApi tokenApi,
            MeterDataApi meterDataApi,
            IsAliveApi isAliveApi,
            MeteringPointsApi meteringPointsApi
    ) {
        this.apiClient = apiClient;
        this.tokenApi = tokenApi;
        this.meterDataApi = meterDataApi;
        this.isAliveApi = isAliveApi;
        this.meteringPointsApi = meteringPointsApi;
    }

    @Override
    public Mono<Boolean> isAlive() {
        synchronized (apiClient) {
            return isAliveApi.customerapiApiIsaliveGet("1.0");
        }
    }

    @Override
    public Mono<String> accessToken(String refreshToken) {
        synchronized (apiClient) {
            setApiKey(refreshToken);
            return tokenApi.customerapiApiTokenGet("1.0")
                           .mapNotNull(StringApiResponse::getResult);
        }
    }

    @Override
    public Mono<MyEnergyDataMarketDocumentResponseListApiResponse> getTimeSeries(
            LocalDate dateFrom,
            LocalDate dateTo,
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
                    meterDataApi.customerapiApiMeterdataGettimeseriesDateFromDateToAggregationPost(
                            dateFrom.format(DateTimeFormatter.ISO_DATE),
                            dateTo.format(DateTimeFormatter.ISO_DATE),
                            aggregation.toString(),
                            "1.0",
                            meteringPointsRequest
                    );
        }
    }

    @Override
    public Mono<MeteringPointDetailsCustomerDtoResponseListApiResponse> getMeteringPointDetails(
            MeteringPointsRequest meteringPointsRequest,
            String accessToken
    ) {
        synchronized (apiClient) {
            setApiKey(accessToken);
            return meteringPointsApi.customerapiApiMeteringpointsMeteringpointGetdetailsPost("1.0",
                                                                                             meteringPointsRequest);
        }
    }

    private void throwIfInvalidTimeframe(LocalDate start, LocalDate end) throws DateTimeException {
        LocalDate currentDate = LocalDate.ofInstant(Instant.now(), DK_ZONE_ID);

        if (start.isEqual(end) || start.isAfter(end)) {
            throw new DateTimeException("Start date must be before end date.");
        }
        if (end.isAfter(currentDate)) {
            throw new DateTimeException("The end date parameter must be <= than the current date.");
        }
        if (start.plusDays(MAX_REQUEST_PERIOD).isBefore(end)) {
            throw new DateTimeException("Request period exceeds the maximum number of days (" + MAX_REQUEST_PERIOD + ").");
        }
    }

    private void setApiKey(String token) {
        apiClient.setBearerToken(token);
    }
}
