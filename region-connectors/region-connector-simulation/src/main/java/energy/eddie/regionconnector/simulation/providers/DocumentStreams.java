package energy.eddie.regionconnector.simulation.providers;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.agnostic.ConnectionStatusMessageProvider;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.v0_82.PermissionMarketDocumentProvider;
import energy.eddie.api.v0_82.ValidatedHistoricalDataEnvelopeProvider;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.regionconnector.simulation.dtos.SimulatedMeterReading;
import energy.eddie.regionconnector.simulation.permission.request.IntermediateValidatedHistoricalDataMarketDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class DocumentStreams implements ValidatedHistoricalDataEnvelopeProvider, ConnectionStatusMessageProvider, PermissionMarketDocumentProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentStreams.class);
    private final Sinks.Many<SimulatedMeterReading> vhdSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<ConnectionStatusMessage> csmSink = Sinks.many().multicast()
                                                                     .onBackpressureBuffer();
    private final Sinks.Many<PermissionEnvelope> pmdSink = Sinks.many().multicast().onBackpressureBuffer();
    private final CommonInformationModelConfiguration cimConfig;

    public DocumentStreams(@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") CommonInformationModelConfiguration cimConfig) {this.cimConfig = cimConfig;}

    public synchronized void publish(SimulatedMeterReading document) {
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
        return getSimulatedMeterReadingStream()
                .map(d -> new IntermediateValidatedHistoricalDataMarketDocument(d, cimConfig))
                .map(IntermediateValidatedHistoricalDataMarketDocument::value);
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

    public Flux<SimulatedMeterReading> getSimulatedMeterReadingStream() {
        return vhdSink.asFlux();
    }
}
