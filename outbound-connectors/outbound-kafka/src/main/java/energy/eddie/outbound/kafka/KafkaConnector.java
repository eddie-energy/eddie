package energy.eddie.outbound.kafka;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.api.agnostic.outbound.ConnectionStatusMessageOutboundConnector;
import energy.eddie.api.agnostic.outbound.RawDataOutboundConnector;
import energy.eddie.api.v0_82.outbound.AccountingPointEnvelopeOutboundConnector;
import energy.eddie.api.v0_82.outbound.PermissionMarketDocumentOutboundConnector;
import energy.eddie.api.v0_82.outbound.ValidatedHistoricalDataEnvelopeOutboundConnector;
import energy.eddie.api.v1_04.outbound.NearRealTimeDataMarketDocumentOutboundConnector;
import energy.eddie.api.v1_04.outbound.ValidatedHistoricalDataMarketDocumentOutboundConnector;
import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.cim.v0_82.ap.MessageDocumentHeaderMetaInformationComplexType;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.cim.v1_04.rtd.RTDEnvelope;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.outbound.shared.Headers;
import energy.eddie.outbound.shared.TopicConfiguration;
import energy.eddie.outbound.shared.TopicStructure;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class KafkaConnector implements
        ConnectionStatusMessageOutboundConnector,
        ValidatedHistoricalDataEnvelopeOutboundConnector,
        PermissionMarketDocumentOutboundConnector,
        RawDataOutboundConnector,
        AccountingPointEnvelopeOutboundConnector,
        ValidatedHistoricalDataMarketDocumentOutboundConnector,
        NearRealTimeDataMarketDocumentOutboundConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaConnector.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final TopicConfiguration config;

    public KafkaConnector(KafkaTemplate<String, Object> kafkaTemplate, TopicConfiguration config) {
        this.kafkaTemplate = kafkaTemplate;
        this.config = config;
    }

    @Override
    public void setConnectionStatusMessageStream(Flux<ConnectionStatusMessage> statusMessageStream) {
        statusMessageStream
                .onBackpressureBuffer()
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(this::produceStatusMessage)
                .onErrorContinue(this::logStreamerError)
                .subscribe();
    }

    @Override
    public void setPermissionMarketDocumentStream(Flux<PermissionEnvelope> permissionMarketDocumentStream) {
        permissionMarketDocumentStream
                .onBackpressureBuffer()
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(this::producePermissionMarketDocument)
                .onErrorContinue(this::logStreamerError)
                .subscribe();
    }

    @Override
    public void setEddieValidatedHistoricalDataMarketDocumentStream(
            Flux<ValidatedHistoricalDataEnvelope> marketDocumentStream
    ) {
        marketDocumentStream
                .onBackpressureBuffer()
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(this::produceEddieValidatedHistoricalDataMarketDocument)
                .onErrorContinue(this::logStreamerError)
                .subscribe();
    }

    @Override
    public void setRawDataStream(Flux<RawDataMessage> rawDataStream) {
        rawDataStream
                .onBackpressureBuffer()
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(this::produceRawDataMessage)
                .onErrorContinue(this::logStreamerError)
                .subscribe();
    }

    @Override
    public void setAccountingPointEnvelopeStream(Flux<AccountingPointEnvelope> marketDocumentStream) {
        marketDocumentStream
                .onBackpressureBuffer()
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(this::produceAccountingPointEnvelope)
                .onErrorContinue(this::logStreamerError)
                .subscribe();
    }

    @Override
    public void setValidatedHistoricalDataMarketDocumentStream(Flux<VHDEnvelope> marketDocumentStream) {
        marketDocumentStream
                .onBackpressureBuffer()
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(this::produceValidatedHistoricalDataMarketDocument)
                .onErrorContinue(this::logStreamerError)
                .subscribe();
    }


    @Override
    public void setNearRealTimeDataMarketDocumentStream(Flux<RTDEnvelope> marketDocumentStream) {
        marketDocumentStream
                .onBackpressureBuffer()
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(this::produceNearRealTimeDataMarketDocument)
                .onErrorContinue(this::logStreamerError)
                .subscribe();
    }

    private void produceStatusMessage(ConnectionStatusMessage statusMessage) {
        var toSend = new ProducerRecord<String, Object>(
                config.connectionStatusMessage(),
                statusMessage
        );
        sendToKafka(toSend, "Could not produce connection status message");
        LOGGER.debug("Produced connection status {} message for permission request {}",
                     statusMessage.status(),
                     statusMessage.permissionId());
    }

    private void producePermissionMarketDocument(PermissionEnvelope permissionMarketDocument) {
        var header = permissionMarketDocument.getMessageDocumentHeader()
                                             .getMessageDocumentHeaderMetaInformation();
        var permissionId = header.getPermissionid();
        var toSend = new ProducerRecord<String, Object>(
                config.permissionMarketDocument(),
                null,
                permissionId,
                permissionMarketDocument,
                cimToHeaders(header)
        );
        sendToKafka(toSend, "Could not produce permission market document");
    }

    private Iterable<Header> cimToHeaders(energy.eddie.cim.v0_82.pmd.MessageDocumentHeaderMetaInformationComplexType header) {
        return List.of(
                new StringHeader(Headers.PERMISSION_ID, header.getPermissionid()),
                new StringHeader(Headers.CONNECTION_ID, header.getConnectionid()),
                new StringHeader(Headers.DATA_NEED_ID, header.getDataNeedid())
        );
    }

    private void produceEddieValidatedHistoricalDataMarketDocument(
            ValidatedHistoricalDataEnvelope marketDocument
    ) {
        var info = marketDocument.getMessageDocumentHeader()
                                 .getMessageDocumentHeaderMetaInformation();
        var toSend = new ProducerRecord<String, Object>(config.validatedHistoricalDataMarketDocument(TopicStructure.DataModels.CIM_0_82),
                                                        null,
                                                        info.getConnectionid(),
                                                        marketDocument,
                                                        cimToHeaders(info));
        sendToKafka(toSend, "Could not produce validated historical data market document message");
        LOGGER.debug("Produced validated historical data market document message for permission request {}",
                     info.getPermissionid());
    }

    private static Iterable<Header> cimToHeaders(energy.eddie.cim.v0_82.vhd.MessageDocumentHeaderMetaInformationComplexType header) {
        return List.of(
                new StringHeader(Headers.PERMISSION_ID, header.getPermissionid()),
                new StringHeader(Headers.CONNECTION_ID, header.getConnectionid()),
                new StringHeader(Headers.DATA_NEED_ID, header.getDataNeedid())
        );
    }

    @SuppressWarnings("LoggingSimilarMessage")
    private void produceValidatedHistoricalDataMarketDocument(VHDEnvelope vhdEnvelope) {
        var toSend = new ProducerRecord<String, Object>(config.validatedHistoricalDataMarketDocument(TopicStructure.DataModels.CIM_1_04),
                                                        null,
                                                        vhdEnvelope.getMessageDocumentHeaderMetaInformationConnectionId(),
                                                        vhdEnvelope,
                                                        cimToHeaders(vhdEnvelope));
        sendToKafka(toSend, "Could not produce validated historical data market document message");
        LOGGER.debug("Produced validated historical data market document message for permission request {}",
                     vhdEnvelope.getMessageDocumentHeaderMetaInformationPermissionId());
    }

    private static Iterable<Header> cimToHeaders(energy.eddie.cim.v1_04.vhd.VHDEnvelope header) {
        return List.of(
                new StringHeader(Headers.PERMISSION_ID, header.getMessageDocumentHeaderMetaInformationPermissionId()),
                new StringHeader(Headers.CONNECTION_ID, header.getMessageDocumentHeaderMetaInformationConnectionId()),
                new StringHeader(Headers.DATA_NEED_ID, header.getMessageDocumentHeaderMetaInformationDataNeedId())
        );
    }

    private void produceRawDataMessage(RawDataMessage message) {
        var toSend = new ProducerRecord<String, Object>(config.rawDataMessage(),
                                                        message.connectionId(),
                                                        message);
        sendToKafka(toSend, "Could not produce raw data message");
        LOGGER.debug("Produced raw data message for permission request {}", message.permissionId());
    }

    private void produceAccountingPointEnvelope(AccountingPointEnvelope marketDocument) {
        var header = marketDocument.getMessageDocumentHeader()
                                   .getMessageDocumentHeaderMetaInformation();
        var toSend = new ProducerRecord<String, Object>(
                config.accountingPointMarketDocument(),
                null,
                header.getConnectionid(),
                marketDocument,
                cimToHeaders(header)
        );
        sendToKafka(toSend, "Could not produce accounting point market document message");
        LOGGER.debug("Produced accounting point market document message for permission request {}",
                     header.getPermissionid());
    }

    private void produceNearRealTimeDataMarketDocument(RTDEnvelope marketDocument) {
        var toSend = new ProducerRecord<String, Object>(
                config.nearRealTimeDataMarketDocument(),
                null,
                marketDocument.getMessageDocumentHeaderMetaInformationConnectionId(),
                marketDocument,
                cimToHeaders(marketDocument)
        );
        sendToKafka(toSend, "Could not produce near real-time market document message");
        LOGGER.debug("Produced near real-time market document message for permission request {}",
                     marketDocument.getMessageDocumentHeaderMetaInformationPermissionId());
    }

    private Iterable<Header> cimToHeaders(energy.eddie.cim.v1_04.rtd.RTDEnvelope header) {
        return List.of(
                new StringHeader(Headers.PERMISSION_ID, header.getMessageDocumentHeaderMetaInformationPermissionId()),
                new StringHeader(Headers.CONNECTION_ID, header.getMessageDocumentHeaderMetaInformationConnectionId()),
                new StringHeader(Headers.DATA_NEED_ID, header.getMessageDocumentHeaderMetaInformationDataNeedId())
        );
    }

    private void sendToKafka(ProducerRecord<String, Object> toSend, String errorMessage) {
        Mono.fromFuture(kafkaTemplate.send(toSend))
            .subscribe(
                    result -> LOGGER.debug("Produced kafka message: {}", result),
                    ex -> LOGGER.warn(errorMessage, ex)
            );
    }

    private void logStreamerError(Throwable throwable, Object obj) {
        LOGGER.warn("Error processing stream object: {}", obj, throwable);
    }

    private static List<Header> cimToHeaders(
            MessageDocumentHeaderMetaInformationComplexType header
    ) {
        return List.of(
                new StringHeader(Headers.PERMISSION_ID, header.getPermissionid()),
                new StringHeader(Headers.CONNECTION_ID, header.getConnectionid()),
                new StringHeader(Headers.DATA_NEED_ID, header.getDataNeedid())
        );
    }

    private record StringHeader(String key, String payload) implements Header {
        @Override
        public byte[] value() {
            return payload.getBytes(StandardCharsets.UTF_8);
        }
    }
}
