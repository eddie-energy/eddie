// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.providers;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.agnostic.ConnectionStatusMessage;
import energy.eddie.cim.agnostic.RawDataMessage;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.cim.v1_12.rpmd.RequestPermissionEnvelope;
import energy.eddie.regionconnector.simulation.SimulationDataSourceInformation;
import energy.eddie.regionconnector.simulation.dtos.SimulatedMeterReading;
import energy.eddie.regionconnector.simulation.permission.request.IntermediateValidatedHistoricalDataMarketDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import tools.jackson.databind.ObjectMapper;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Component
public class DocumentStreams implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentStreams.class);
    private final Sinks.Many<SimulatedMeterReading> vhdSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<ConnectionStatusMessage> csmSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<PermissionEnvelope> pmdSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<String> terminationSink = Sinks.many().replay().latest();
    private final CommonInformationModelConfiguration cimConfig;
    private final ObjectMapper objectMapper;
    private final Sinks.Many<RequestPermissionEnvelope> rpmdSink = Sinks.many().multicast().onBackpressureBuffer();

    public DocumentStreams(
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") CommonInformationModelConfiguration cimConfig,
            @Qualifier("jacksonJsonMapper") ObjectMapper objectMapper
    ) {
        this.cimConfig = cimConfig;
        this.objectMapper = objectMapper;
    }

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

    public synchronized void publish(RequestPermissionEnvelope permissionMarketDocument) {
        rpmdSink.tryEmitNext(permissionMarketDocument);
    }

    public synchronized void publish(String permissionToBeTerminated) {
        terminationSink.tryEmitNext(permissionToBeTerminated);
    }

    @MessageStream(ValidatedHistoricalDataEnvelope.class)
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
        terminationSink.tryEmitComplete();
    }

    @MessageStream(ConnectionStatusMessage.class)
    public Flux<ConnectionStatusMessage> getConnectionStatusMessageStream() {
        return csmSink.asFlux();
    }

    @MessageStream(PermissionEnvelope.class)
    public Flux<PermissionEnvelope> getPermissionMarketDocumentStream() {
        return pmdSink.asFlux();
    }

    @MessageStream(RequestPermissionEnvelope.class)
    public Flux<RequestPermissionEnvelope> getRequestPermissionMarketDocumentStream() {
        return rpmdSink.asFlux();
    }

    @MessageStream(RawDataMessage.class)
    public Flux<RawDataMessage> getRawDataMessageStream() {
        return getSimulatedMeterReadingStream()
                .map(data -> new RawDataMessage(
                        data.permissionId(),
                        data.connectionId(),
                        data.dataNeedId(),
                        new SimulationDataSourceInformation(),
                        ZonedDateTime.now(ZoneOffset.UTC),
                        objectMapper.writeValueAsString(data)));
    }

    public Flux<SimulatedMeterReading> getSimulatedMeterReadingStream() {
        return vhdSink.asFlux();
    }

    public Flux<String> getTerminationStream() {
        return terminationSink.asFlux();
    }
}
