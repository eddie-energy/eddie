package energy.eddie.regionconnector.us.green.button.services;

import com.rometools.rome.feed.synd.SyndFeed;
import energy.eddie.api.agnostic.IdentifiablePayload;
import energy.eddie.regionconnector.us.green.button.permission.request.api.UsGreenButtonPermissionRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class PublishService implements AutoCloseable {
    private final Sinks.Many<IdentifiablePayload<UsGreenButtonPermissionRequest, SyndFeed>> vhdFeeds = Sinks.many()
                                                                                                            .multicast()
                                                                                                            .onBackpressureBuffer();
    private final Sinks.Many<IdentifiablePayload<UsGreenButtonPermissionRequest, SyndFeed>> apFeeds = Sinks.many()
                                                                                                            .multicast()
                                                                                                            .onBackpressureBuffer();

    public void publishValidatedHistoricalData(IdentifiablePayload<UsGreenButtonPermissionRequest, SyndFeed> identifiablePayload) {
        vhdFeeds.tryEmitNext(identifiablePayload);
    }

    public Flux<IdentifiablePayload<UsGreenButtonPermissionRequest, SyndFeed>> validatedHistoricalData() {
        return vhdFeeds.asFlux();
    }

    public void publishAccountingPointData(IdentifiablePayload<UsGreenButtonPermissionRequest, SyndFeed> identifiablePayload) {
        apFeeds.tryEmitNext(identifiablePayload);
    }

    public Flux<IdentifiablePayload<UsGreenButtonPermissionRequest, SyndFeed>> accountingPointData() {
        return apFeeds.asFlux();
    }

    @Override
    public void close() {
        vhdFeeds.tryEmitComplete();
    }
}
