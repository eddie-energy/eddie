package energy.eddie.regionconnector.us.green.button.services;

import com.rometools.rome.feed.synd.SyndFeed;
import energy.eddie.api.agnostic.IdentifiablePayload;
import energy.eddie.regionconnector.us.green.button.permission.request.api.UsGreenButtonPermissionRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class PublishService implements AutoCloseable {
    private final Sinks.Many<IdentifiablePayload<UsGreenButtonPermissionRequest, SyndFeed>> feeds = Sinks.many()
                                                                                                         .multicast()
                                                                                                         .onBackpressureBuffer();

    public void publish(IdentifiablePayload<UsGreenButtonPermissionRequest, SyndFeed> identifiablePayload) {
        feeds.tryEmitNext(identifiablePayload);
    }

    public Flux<IdentifiablePayload<UsGreenButtonPermissionRequest, SyndFeed>> flux() {
        return feeds.asFlux();
    }


    @Override
    public void close() {
        feeds.tryEmitComplete();
    }
}
