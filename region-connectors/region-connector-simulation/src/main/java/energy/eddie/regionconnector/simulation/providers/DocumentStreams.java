package energy.eddie.regionconnector.simulation.providers;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.agnostic.ConnectionStatusMessageProvider;
import energy.eddie.api.v0_82.PermissionMarketDocumentProvider;
import energy.eddie.api.v0_82.ValidatedHistoricalDataEnvelopeProvider;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class DocumentStreams implements ValidatedHistoricalDataEnvelopeProvider, ConnectionStatusMessageProvider, PermissionMarketDocumentProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentStreams.class);
    private final Sinks.Many<ValidatedHistoricalDataEnvelope> vhdSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<ConnectionStatusMessage> csmSink = Sinks.many().multicast()
                                                                     .onBackpressureBuffer();
    private final Sinks.Many<PermissionEnvelope> pmdSink = Sinks.many().multicast().onBackpressureBuffer();

    public synchronized void publish(ValidatedHistoricalDataEnvelope document) {
        LOGGER.info("Publishing validated historical data market document");
        vhdSink.tryEmitNext(document);
    }

    public synchronized void publish(ConnectionStatusMessage connectionStatusMessage) {
        csmSink.tryEmitNext(connectionStatusMessage);
    }

    public synchronized void publish(PermissionEnvelope permissionEnvelope) {
        pmdSink.tryEmitNext(permissionEnvelope);
    }

    @Override
    public Flux<ValidatedHistoricalDataEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return vhdSink.asFlux();
    }

    @Override
    public void close() {
        vhdSink.tryEmitComplete();
        pmdSink.tryEmitComplete();
        csmSink.tryEmitComplete();
    }

    @Override
    public Flux<ConnectionStatusMessage> getConnectionStatusMessageStream() {
        return csmSink.asFlux();
    }

    @Override
    public Flux<PermissionEnvelope> getPermissionMarketDocumentStream() {
        return pmdSink.asFlux();
    }
}
