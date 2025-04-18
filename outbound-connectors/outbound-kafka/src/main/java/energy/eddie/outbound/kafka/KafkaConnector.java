package energy.eddie.outbound.kafka;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.api.agnostic.outbound.ConnectionStatusMessageOutboundConnector;
import energy.eddie.api.agnostic.outbound.RawDataOutboundConnector;
import energy.eddie.api.v0_82.outbound.AccountingPointEnvelopeOutboundConnector;
import energy.eddie.api.v0_82.outbound.PermissionMarketDocumentOutboundConnector;
import energy.eddie.api.v0_82.outbound.ValidatedHistoricalDataEnvelopeOutboundConnector;
import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.cim.v0_82.ap.MessageDocumentHeaderMetaInformationComplexType;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
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
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class KafkaConnector implements
        ConnectionStatusMessageOutboundConnector,
        ValidatedHistoricalDataEnvelopeOutboundConnector,
        PermissionMarketDocumentOutboundConnector,
        RawDataOutboundConnector,
        AccountingPointEnvelopeOutboundConnector {
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

    private void produceStatusMessage(ConnectionStatusMessage statusMessage) {
        var toSend = new ProducerRecord<String, Object>(
                config.toEddieProductionTopic(TopicStructure.DataModels.AGNOSTIC,
                                              TopicStructure.DocumentTypes.CONNECTION_STATUS_MESSAGE),
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
                config.toEddieProductionTopic(TopicStructure.DataModels.CIM_0_82,
                                              TopicStructure.DocumentTypes.PERMISSION_MD),
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
        var toSend = new ProducerRecord<String, Object>(config.toEddieProductionTopic(TopicStructure.DataModels.CIM_0_82,
                                                                                      TopicStructure.DocumentTypes.VALIDATED_HISTORICAL_DATA_MD),
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

    private void produceRawDataMessage(RawDataMessage message) {
        var toSend = new ProducerRecord<String, Object>(config.toEddieProductionTopic(TopicStructure.DataModels.AGNOSTIC,
                                                                                      TopicStructure.DocumentTypes.RAW_DATA_MESSAGE),
                                                        message.connectionId(),
                                                        message);
        sendToKafka(toSend, "Could not produce raw data message");
        LOGGER.debug("Produced raw data message for permission request {}", message.permissionId());
    }

    private void produceAccountingPointEnvelope(AccountingPointEnvelope marketDocument) {
        var header = marketDocument.getMessageDocumentHeader()
                                   .getMessageDocumentHeaderMetaInformation();
        var toSend = new ProducerRecord<String, Object>(
                config.toEddieProductionTopic(TopicStructure.DataModels.CIM_0_82,
                                              TopicStructure.DocumentTypes.ACCOUNTING_POINT_MD),
                null,
                header.getConnectionid(),
                marketDocument,
                cimToHeaders(header)
        );
        sendToKafka(toSend, "Could not produce accounting point market document message");
        LOGGER.debug("Produced accounting point market document message for permission request {}",
                     header.getPermissionid());
    }

    private void sendToKafka(ProducerRecord<String, Object> toSend, String errorMessage) {
        kafkaTemplate.send(toSend).whenComplete((result, ex) -> {
            if (ex != null) {
                LOGGER.warn(errorMessage, ex);
            } else {
                LOGGER.debug("Produced kafka message: {}", result);
            }
        });
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
