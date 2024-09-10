package energy.eddie.regionconnector.us.green.button.client;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import energy.eddie.regionconnector.us.green.button.api.GreenButtonApi;
import energy.eddie.regionconnector.us.green.button.exceptions.DataNotReadyException;
import energy.eddie.regionconnector.us.green.button.xml.helper.Status;
import org.naesb.espi.ServiceStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.StringReader;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

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
                            .uri("/ReadServiceStatus")
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
    public Mono<SyndFeed> batchSubscription(
            String authId,
            String accessToken,
            ZonedDateTime publishedMin,
            ZonedDateTime publishedMax
    ) {
        LOGGER.info("Polling for batch subscription");
        synchronized (webClient) {
            return webClient.get()
                            .uri(builder -> builder.path("/Batch/Subscription/")
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
