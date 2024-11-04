package energy.eddie.regionconnector.us.green.button.client;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import energy.eddie.regionconnector.us.green.button.api.GreenButtonApi;
import energy.eddie.regionconnector.us.green.button.api.Pages;
import energy.eddie.regionconnector.us.green.button.client.dtos.MeterListing;
import energy.eddie.regionconnector.us.green.button.client.dtos.authorization.Authorization;
import energy.eddie.regionconnector.us.green.button.client.dtos.meter.HistoricalCollectionResponse;
import energy.eddie.regionconnector.us.green.button.client.dtos.meter.MeterList;
import energy.eddie.regionconnector.us.green.button.xml.helper.Status;
import jakarta.annotation.Nullable;
import org.naesb.espi.ServiceStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.StringReader;
import java.net.URI;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
public class GreenButtonClient implements GreenButtonApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(GreenButtonClient.class);
    private final WebClient webClient;
    private final SyndFeedInput input = new SyndFeedInput();
    private final AtomFeedTransformer atomFeedTransformer = new AtomFeedTransformer();

    public GreenButtonClient(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public Mono<ServiceStatus> readServiceStatus() {
        synchronized (webClient) {
            return webClient.get()
                            .uri("/DataCustodian/espi/1_1/resource/ReadServiceStatus")
                            .retrieve()
                            .bodyToMono(ServiceStatus.class);
        }
    }

    @Override
    public Mono<Boolean> isAlive() {
        return readServiceStatus().map(serviceStatus -> !Status.fromValue(serviceStatus.getCurrentStatus())
                                                               .equals(Status.UNAVAILABLE));
    }

    @Override
    public Flux<SyndFeed> batchSubscription(
            String authId,
            String accessToken,
            Iterable<String> meters,
            ZonedDateTime publishedMin,
            ZonedDateTime publishedMax
    ) {
        return Flux.fromIterable(meters)
                   .flatMap(meter -> batchSubscription(authId, accessToken, publishedMin, publishedMax, meter));
    }

    @Override
    public Mono<HistoricalCollectionResponse> collectHistoricalData(List<String> meterIds) {
        LOGGER.info("Triggering historical data collection for meters {}", meterIds);
        var meterList = new MeterList(meterIds);
        synchronized (webClient) {
            return webClient.post()
                            .uri("/api/v2/meters/historical-collection")
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(meterList)
                            .retrieve()
                            .bodyToMono(HistoricalCollectionResponse.class);
        }
    }

    @Override
    public Flux<MeterListing> fetchInactiveMeters(Pages slurp, List<String> authIds) {
        LOGGER.info("Fetching accounting point JSON data for following meters with authorization IDs {}", authIds);
        synchronized (webClient) {
            return webClient.get()
                            .uri(builder -> builder.path("/api/v2/meters")
                                                   .queryParam("is_activated", false)
                                                   .queryParam("authorizations", authIds)
                                                   .build())
                            .retrieve()
                            .bodyToMono(MeterListing.class)
                            .expand(response -> {
                                if (response.next() == null || !slurp.value()) {
                                    return Mono.empty();
                                }
                                return fetchAdditionalMeters(response.next());
                            });
        }
    }

    @Override
    public Mono<Authorization> revoke(String authUid) {
        LOGGER.info("Triggering revocation for authorization UID {}", authUid);
        synchronized (webClient) {
            return webClient.post()
                            .uri("/api/v2/authorizations/{authUid}/revoke", Map.of("authUid", authUid))
                            .retrieve()
                            .bodyToMono(Authorization.class);
        }
    }

    private Mono<SyndFeed> batchSubscription(
            String authId,
            String accessToken,
            ZonedDateTime publishedMin,
            ZonedDateTime publishedMax,
            String meter
    ) {
        LOGGER.info("Polling for batch subscription for meter {}", meter);
        synchronized (webClient) {
            return batchSubscriptionApiCall(authId, accessToken, publishedMin, publishedMax, meter)
                    .expand(prevResponse -> {
                        var header = prevResponse.headers().header("Retry-After");
                        if (header.isEmpty() || prevResponse.status() != HttpStatus.ACCEPTED) {
                            return Mono.empty();
                        }
                        long delayInSeconds = Long.parseLong(header.getFirst());
                        LOGGER.info("Batch Subscription not available yet for authUid {}, retrying after {}s",
                                    authId,
                                    delayInSeconds);
                        return Mono.delay(Duration.ofSeconds(delayInSeconds))
                                   .then(batchSubscriptionApiCall(authId,
                                                                  accessToken,
                                                                  publishedMin,
                                                                  publishedMax,
                                                                  meter));
                    })
                    .mapNotNull(ResponseWithHeaders::rawResponse)
                    .flatMap(this::parsePayload)
                    .last();
        }
    }

    private Mono<ResponseWithHeaders> batchSubscriptionApiCall(
            String authId,
            String accessToken,
            ZonedDateTime publishedMin,
            ZonedDateTime publishedMax,
            String meter
    ) {
        return webClient.get()
                        .uri(builder -> builder.path("/DataCustodian/espi/1_1/resource/Batch/Subscription/")
                                               .path(authId)
                                               .path("/UsagePoint/")
                                               .path(meter)
                                               .queryParam("published-min",
                                                           publishedMin.format(
                                                                   DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                                               .queryParam("published-max",
                                                           publishedMax.format(
                                                                   DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                                               .build()
                        )
                        .header("Authorization", "Bearer " + accessToken)
                        .exchangeToMono(response -> response.bodyToMono(String.class)
                                                            .map(rawResponse -> new ResponseWithHeaders(
                                                                    rawResponse,
                                                                    response.headers(),
                                                                    response.statusCode()
                                                            ))
                                                            .defaultIfEmpty(new ResponseWithHeaders(null,
                                                                                                    response.headers(),
                                                                                                    response.statusCode())));
    }

    private Mono<MeterListing> fetchAdditionalMeters(URI url) {
        return webClient.get().uri(url).retrieve().bodyToMono(MeterListing.class);
    }


    private Mono<SyndFeed> parsePayload(String payload) {
        try {
            payload = atomFeedTransformer.transform(payload);
            SyndFeed feed = input.build(new StringReader(payload));
            return Mono.just(feed);
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    private record ResponseWithHeaders(@Nullable String rawResponse, ClientResponse.Headers headers,
                                       HttpStatusCode status) {}
}
