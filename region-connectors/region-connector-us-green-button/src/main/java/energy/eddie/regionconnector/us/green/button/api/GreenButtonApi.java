package energy.eddie.regionconnector.us.green.button.api;

import com.rometools.rome.feed.synd.SyndFeed;
import org.naesb.espi.ServiceStatus;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;

public interface GreenButtonApi {
    Mono<ServiceStatus> readServiceStatus();

    Mono<Boolean> isAlive();

    Mono<SyndFeed> batchSubscription(
            String authId,
            String accessToken,
            ZonedDateTime publishedMin,
            ZonedDateTime publishedMax
    );
}
