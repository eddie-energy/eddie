package energy.eddie.regionconnector.de.eta.streams;

import energy.eddie.regionconnector.de.eta.permission.requests.DeEtaPermissionRequest;
import energy.eddie.regionconnector.de.eta.providers.apd.IdentifiableAccountingPointData;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class AccountingPointDataStream {
    private final Sinks.Many<IdentifiableAccountingPointData> sink = Sinks.many().multicast().onBackpressureBuffer();

    public Flux<IdentifiableAccountingPointData> accountingPointData() {
        return sink.asFlux();
    }

    public void publish(DeEtaPermissionRequest pr, Object rawData) {
        sink.tryEmitNext(new IdentifiableAccountingPointData(pr, rawData));
    }
}