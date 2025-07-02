package energy.eddie.regionconnector.us.green.button.providers.agnostic;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedOutput;
import energy.eddie.api.agnostic.IdentifiablePayload;
import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.api.agnostic.RawDataProvider;
import energy.eddie.regionconnector.us.green.button.permission.request.api.UsGreenButtonPermissionRequest;
import energy.eddie.regionconnector.us.green.button.services.PublishService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class UsRawDataProvider implements RawDataProvider {
    private final SyndFeedOutput output = new SyndFeedOutput();
    private final PublishService publishService;

    public UsRawDataProvider(PublishService publishService) {this.publishService = publishService;}

    @Override
    public Flux<RawDataMessage> getRawDataStream() {
        return publishService.validatedHistoricalData()
                             .mergeWith(publishService.accountingPointData())
                             .flatMap(this::serializePayload);
    }

    @Override
    public void close() {
        // No-Op
    }

    private Mono<RawDataMessage> serializePayload(IdentifiablePayload<UsGreenButtonPermissionRequest, SyndFeed> id) {
        try {
            var rawPayload = output.outputString(id.payload());
            return Mono.just(new RawDataMessage(
                    id.permissionRequest(),
                    rawPayload
            ));
        } catch (FeedException e) {
            return Mono.error(e);
        }
    }
}
