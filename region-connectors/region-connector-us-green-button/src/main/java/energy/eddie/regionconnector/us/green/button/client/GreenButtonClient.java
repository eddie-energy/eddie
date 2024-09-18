package energy.eddie.regionconnector.us.green.button.client;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import energy.eddie.regionconnector.us.green.button.api.GreenButtonApi;
import energy.eddie.regionconnector.us.green.button.api.Pages;
import energy.eddie.regionconnector.us.green.button.client.dtos.HistoricalCollectionResponse;
import energy.eddie.regionconnector.us.green.button.client.dtos.MeterList;
import energy.eddie.regionconnector.us.green.button.client.dtos.MeterListing;
import energy.eddie.regionconnector.us.green.button.exceptions.DataNotReadyException;
import energy.eddie.regionconnector.us.green.button.xml.helper.Status;
import org.naesb.espi.ServiceStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.StringReader;
import java.net.URI;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
    public Mono<SyndFeed> batchSubscription(
            String authId,
            String accessToken,
            ZonedDateTime publishedMin,
            ZonedDateTime publishedMax
    ) {
        LOGGER.info("Polling for batch subscription");
        synchronized (webClient) {
            return webClient.get()
                            .uri(builder -> builder.path("/DataCustodian/espi/1_1/resource/Batch/Subscription/")
                                                   .path(authId)
                                                   .queryParam("published-min",
                                                               publishedMin.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                                                   .queryParam("published-max",
                                                               publishedMax.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                                                   .build()
                            )
                            .header("Authorization", "Bearer " + accessToken)
                            .retrieve()
                            .onStatus(status -> status == HttpStatus.ACCEPTED,
                                      resp -> Mono.just(new DataNotReadyException()))
                            .bodyToMono(String.class)
                            .flatMap(this::parsePayload);
        }
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
    public Mono<Boolean> isAlive() {
        return readServiceStatus().map(serviceStatus -> !Status.fromValue(serviceStatus.getCurrentStatus())
                                                               .equals(Status.UNAVAILABLE));
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
}
