package energy.eddie.regionconnector.dk.energinet.customer.client;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointDetailsCustomerDtoResponseListApiResponse;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointsRequest;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocumentResponseListApiResponse;
import energy.eddie.regionconnector.dk.energinet.customer.model.StringApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ApiVersionInserter;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;

import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata.DK_ZONE_ID;

@Component
public class EnerginetCustomerApiClient {
    // The Request period must not exceed the maximum number of days of 730
    private static final int MAX_REQUEST_PERIOD = 730;
    private final WebClient webClient;

    @Autowired
    public EnerginetCustomerApiClient(EnerginetConfiguration configuration, WebClient.Builder builder) {
        this.webClient = builder.baseUrl(configuration.customerBasePath())
                                .apiVersionInserter(ApiVersionInserter.useHeader("api-version"))
                                .defaultApiVersion("1.0")
                                .build();
    }

    public Mono<Boolean> isAlive() {
        return webClient.get()
                        .uri("/customerapi/api/isalive")
                        .exchangeToMono(response -> {
                            var statusCode = response.statusCode();
                            if (statusCode.is2xxSuccessful()
                                || HttpStatus.SERVICE_UNAVAILABLE.equals(statusCode)
                                || HttpStatus.BAD_REQUEST.equals(statusCode)) {
                                return response.bodyToMono(Boolean.class);
                            }
                            return Mono.just(false);
                        });
    }

    public Mono<String> accessToken(String refreshToken) {
        return webClient.get()
                        .uri("/customerapi/api/token")
                        .headers(headers -> headers.setBearerAuth(refreshToken))
                        .retrieve()
                        .bodyToMono(StringApiResponse.class)
                        .mapNotNull(StringApiResponse::getResult);
    }

    public Mono<MyEnergyDataMarketDocumentResponseListApiResponse> getTimeSeries(
            LocalDate dateFrom,
            LocalDate dateTo,
            Granularity granularity,
            MeteringPointsRequest meteringPointsRequest,
            String accessToken
    ) {
        throwIfInvalidTimeframe(dateFrom, dateTo);
        TimeSeriesAggregationEnum aggregation = TimeSeriesAggregationEnum.fromGranularity(granularity);
        return webClient.post()
                        .uri("/customerapi/api/meterdata/gettimeseries/{dateFrom}/{dateTo}/{aggregation}",
                             dateFrom,
                             dateTo,
                             aggregation.toString())
                        .headers(headers -> headers.setBearerAuth(accessToken))
                        .bodyValue(meteringPointsRequest)
                        .retrieve()
                        .bodyToMono(MyEnergyDataMarketDocumentResponseListApiResponse.class);
    }

    public Mono<MeteringPointDetailsCustomerDtoResponseListApiResponse> getMeteringPointDetails(
            MeteringPointsRequest meteringPointsRequest,
            String accessToken
    ) {
        return webClient.post()
                        .uri("/customerapi/api/meteringpoints/meteringpoint/getdetails")
                        .headers(headers -> headers.setBearerAuth(accessToken))
                        .bodyValue(meteringPointsRequest)
                        .retrieve()
                        .bodyToMono(MeteringPointDetailsCustomerDtoResponseListApiResponse.class);
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
}
