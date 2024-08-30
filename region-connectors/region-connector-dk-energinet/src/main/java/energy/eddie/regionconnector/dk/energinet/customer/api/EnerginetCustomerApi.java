package energy.eddie.regionconnector.dk.energinet.customer.api;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.HealthState;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointDetailsCustomerDtoResponseListApiResponse;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointsRequest;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocumentResponseListApiResponse;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public interface EnerginetCustomerApi {
    /**
     * Call this endpoint to verify whether Energinet is currently operating normally. If Energinet is experiencing more
     * traffic than it can handle, this endpoint may return HTTP 503.  The status currently refreshes every 60 seconds.
     *
     * @return Boolean
     */
    Mono<Boolean> isAlive();

    Mono<String> accessToken(String refreshToken);

    /**
     * Returns the time series for each metering point in list.
     *
     * @param dateFrom              Date from filter in format: 'YYYY-MM-DD' (required)
     * @param dateTo                Date to filter in format: 'YYYY-MM-DD' (required)
     * @param granularity           Granularity. Supported values are: 'PT15M', 'PT1H', 'PT1D', 'P1M', 'P1Y' (required)
     * @param meteringPointsRequest List of metering point ids. (optional)
     * @return MyEnergyDataMarketDocumentResponseListApiResponse from the server.
     */
    Mono<MyEnergyDataMarketDocumentResponseListApiResponse> getTimeSeries(
            LocalDate dateFrom,
            LocalDate dateTo,
            Granularity granularity,
            MeteringPointsRequest meteringPointsRequest,
            String accessToken,
            UUID correlationId
    );

    /**
     * Returns the details for each metering point in the list.
     *
     * @param meteringPointsRequest List of metering point ids.
     * @param accessToken           Access token.
     * @return MeteringPointDetailsCustomerDtoResponseListApiResponse from the server.
     */
    Mono<MeteringPointDetailsCustomerDtoResponseListApiResponse> getMeteringPointDetails(
            MeteringPointsRequest meteringPointsRequest,
            String accessToken
    );

    Mono<Map<String, HealthState>> health();
}
