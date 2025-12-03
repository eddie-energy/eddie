package energy.eddie.regionconnector.dk.energinet.providers;

import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableAccountingPointDetails;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableApiResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class EnergyDataStreams implements AutoCloseable {
    private final Sinks.Many<IdentifiableApiResponse> vhdSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<IdentifiableAccountingPointDetails> apSink = Sinks.many()
                                                                               .multicast()
                                                                               .onBackpressureBuffer();

    public void publish(IdentifiableApiResponse response) {
        vhdSink.tryEmitNext(response);
    }

    public Flux<IdentifiableApiResponse> getValidatedHistoricalDataStream() {
        return vhdSink.asFlux();
    }

    public void publish(IdentifiableAccountingPointDetails ap) {
        apSink.tryEmitNext(ap);
    }

    public Flux<IdentifiableAccountingPointDetails> getAccountingPointDataStream() {
        return apSink.asFlux();
    }

    @Override
    public void close() {
        vhdSink.tryEmitComplete();
        apSink.tryEmitComplete();
    }
}
