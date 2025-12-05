package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableAccountingPointData;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableMeterReading;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;

@Component
public class EnergyDataStreams implements AutoCloseable {
    private final Sinks.Many<IdentifiableMeterReading> vhdSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<IdentifiableAccountingPointData> apSink = Sinks.many().multicast().onBackpressureBuffer();

    public void publish(IdentifiableMeterReading vhd) {
        vhdSink.emitNext(vhd, Sinks.EmitFailureHandler.busyLooping(Duration.ofMinutes(1)));
    }

    public void publish(IdentifiableAccountingPointData ap) {
        apSink.emitNext(ap, Sinks.EmitFailureHandler.busyLooping(Duration.ofMinutes(1)));
    }

    public Flux<IdentifiableMeterReading> getValidatedHistoricalData() {
        return vhdSink.asFlux();
    }

    public Flux<IdentifiableAccountingPointData> getAccountingPointData() {
        return apSink.asFlux();
    }

    @Override
    public void close() {
        vhdSink.tryEmitComplete();
        apSink.tryEmitComplete();
    }
}
