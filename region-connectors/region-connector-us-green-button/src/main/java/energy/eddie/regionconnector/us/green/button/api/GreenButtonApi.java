package energy.eddie.regionconnector.us.green.button.api;

import com.rometools.rome.feed.synd.SyndFeed;
import energy.eddie.regionconnector.us.green.button.client.dtos.MeterListing;
import energy.eddie.regionconnector.us.green.button.client.dtos.authorization.Authorization;
import energy.eddie.regionconnector.us.green.button.client.dtos.meter.HistoricalCollectionResponse;
import energy.eddie.regionconnector.us.green.button.client.dtos.meter.Meter;
import org.naesb.espi.ServiceStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;
import java.util.List;

public interface GreenButtonApi {

    Mono<ServiceStatus> readServiceStatus();

    Mono<Boolean> isAlive();

    Flux<SyndFeed> batchSubscription(
            String authId,
            String accessToken,
            Iterable<String> meters,
            ZonedDateTime publishedMin,
            ZonedDateTime publishedMax
    );

    Mono<SyndFeed> retailCustomer(String authId, String accessToken);

    /**
     * Triggers historical data collection for the meters.
     *
     * @param meterIds the meters for which the collection should be triggered
     * @param company  the company to which the meters belong to
     * @return the result of the API call
     */
    Mono<HistoricalCollectionResponse> collectHistoricalData(List<String> meterIds, String company);

    /**
     * Fetches all meters that have been shared with this utility user. Uses the JSON API to fetch the meter data: <a
     * href="https://utilityapi.com/docs/api/meters/list">https://utilityapi.com/docs/api/meters/list</a>. The API
     * supports pagination, so it's possible to only request the first page or all pages.
     *
     * @param slurp   if all pages should be requested
     * @param authIds auth IDs of the meters of interest
     * @param company the company which the meters belong to
     * @return shared meter
     */
    Flux<MeterListing> fetchMeters(Pages slurp, List<String> authIds, String company);

    /**
     * Fetches the details for one meter.
     *
     * @param meterId the UID of the meter
     * @param company the company of the meter
     * @return the meter details
     */
    Mono<Meter> fetchMeter(String meterId, String company);

    Mono<Authorization> revoke(String authUid, String company);
}
