package energy.eddie.regionconnector.de.eta.streams;

import energy.eddie.regionconnector.de.eta.permission.requests.DeEtaPermissionRequest;
import energy.eddie.regionconnector.de.eta.providers.vhd.IdentifiableValidatedHistoricalData;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class ValidatedHistoricalDataStream {
    private final Sinks.Many<IdentifiableValidatedHistoricalData> sink = Sinks.many().multicast().onBackpressureBuffer();

    public Flux<IdentifiableValidatedHistoricalData> validatedHistoricalData() {
        return sink.asFlux();
    }

    public void publish(DeEtaPermissionRequest pr, Object rawData) {
        sink.tryEmitNext(new IdentifiableValidatedHistoricalData(pr, rawData));
    }
}