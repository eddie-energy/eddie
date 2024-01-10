package energy.eddie.regionconnector.dk.energinet.customer.api;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.api.v0.HealthState;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointsRequest;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

public interface EnerginetCustomerApi {
    /**
     * Call this endpoint to verify whether Energinet is currently operating normally.
     * If Energinet is experiencing more traffic than we can handle, this endpoint may return HTTP 503.  The status currently refreshes every 60 seconds.
     *
     * @return Boolean
     */
    Mono<Boolean> isAlive();

    Mono<String> accessToken(String refreshToken);

    /**
     * Returns ConsumptionRecord out of a time series for each metering point in list.
     *
     * @param dateFrom              Date from filter in format: 'YYYY-MM-DD' (required)
     * @param dateTo                Date to filter in format: 'YYYY-MM-DD' (required)
     * @param granularity           Granularity. Supported values are: 'PT15M', 'PT1H', 'PT1D', 'P1M', 'P1Y' (required)
     * @param meteringPointsRequest List of metering point ids. (optional)
     * @return MyEnergyDataMarketDocumentResponseListApiResponse
     */
    Mono<ConsumptionRecord> getTimeSeries(ZonedDateTime dateFrom, ZonedDateTime dateTo, Granularity granularity, MeteringPointsRequest meteringPointsRequest, String accessToken, UUID correlationId);

    Mono<Map<String, HealthState>> health();
}