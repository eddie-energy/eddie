package energy.eddie.outbound.kafka;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.agnostic.ConnectionStatusMessageOutboundConnector;
import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.api.agnostic.RawDataOutboundConnector;
import energy.eddie.api.v0_82.outbound.AccountingPointEnvelopeOutboundConnector;
import energy.eddie.api.v0_82.outbound.PermissionMarketDocumentOutboundConnector;
import energy.eddie.api.v0_82.outbound.ValidatedHistoricalDataEnvelopeOutboundConnector;
import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.cim.v0_82.ap.MessageDocumentHeaderMetaInformationComplexType;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Future;

@Component
public class KafkaConnector implements
        ConnectionStatusMessageOutboundConnector,
        ValidatedHistoricalDataEnvelopeOutboundConnector,
        PermissionMarketDocumentOutboundConnector,
        RawDataOutboundConnector,
        AccountingPointEnvelopeOutboundConnector {
    public static final String PERMISSION_ID = "permission-id";
    public static final String CONNECTION_ID = "connection-id";
    public static final String DATA_NEED_ID = "data-need-id";
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaConnector.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaConnector(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void setConnectionStatusMessageStream(Flux<ConnectionStatusMessage> statusMessageStream) {
        statusMessageStream
                .publishOn(Schedulers.boundedElastic())
                .subscribe(this::produceStatusMessage);
    }

    @Override
    public void setPermissionMarketDocumentStream(Flux<PermissionEnvelope> permissionMarketDocumentStream) {
        permissionMarketDocumentStream
                .publishOn(Schedulers.boundedElastic())
                .subscribe(this::producePermissionMarketDocument);
    }

    @Override
    public void setEddieValidatedHistoricalDataMarketDocumentStream(
            Flux<ValidatedHistoricalDataEnvelope> marketDocumentStream
    ) {
        marketDocumentStream
                .publishOn(Schedulers.boundedElastic())
                .subscribe(this::produceEddieValidatedHistoricalDataMarketDocument);
    }

    @Override
    public void setRawDataStream(Flux<RawDataMessage> rawDataStream) {
        rawDataStream
                .publishOn(Schedulers.boundedElastic())
                .subscribe(this::produceRawDataMessage);
    }

    @Override
    public void setAccountingPointEnvelopeStream(Flux<AccountingPointEnvelope> marketDocumentStream) {
        marketDocumentStream
                .publishOn(Schedulers.boundedElastic())
                .subscribe(this::produceAccountingPointEnvelope);
    }

    private void produceStatusMessage(ConnectionStatusMessage statusMessage) {
        var future = kafkaTemplate.send(new ProducerRecord<>("status-messages", statusMessage));
        awaitFuture(future, "Could not produce connection status message");
        LOGGER.debug("Produced connection status {} message for permission request {}",
                     statusMessage.status(),
                     statusMessage.permissionId());
    }

    private static void awaitFuture(Future<SendResult<String, Object>> future, String errorMessage) {
        Thread.startVirtualThread(() -> {
            try {
                LOGGER.debug("Produced kafka message {}", future.get());
            } catch (InterruptedException e) {
                LOGGER.warn(errorMessage, e);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                LOGGER.warn(errorMessage, e);
            }
        });
    }

    private void producePermissionMarketDocument(PermissionEnvelope permissionMarketDocument) {
        var header = permissionMarketDocument.getMessageDocumentHeader()
                                             .getMessageDocumentHeaderMetaInformation();
        var permissionId = header.getPermissionid();
        var toSend = new ProducerRecord<String, Object>(
                "permission-market-documents",
                null,
                permissionId,
                permissionMarketDocument,
                cimToHeaders(header)
        );
        var future = kafkaTemplate.send(toSend);
        awaitFuture(future, "Could not produce permission market document");
    }

    private Iterable<Header> cimToHeaders(energy.eddie.cim.v0_82.pmd.MessageDocumentHeaderMetaInformationComplexType header) {
        return List.of(
                new StringHeader(PERMISSION_ID, header.getPermissionid()),
                new StringHeader(CONNECTION_ID, header.getConnectionid()),
                new StringHeader(DATA_NEED_ID, header.getDataNeedid())
        );
    }

    private void produceEddieValidatedHistoricalDataMarketDocument(
            ValidatedHistoricalDataEnvelope marketDocument
    ) {
        var info = marketDocument.getMessageDocumentHeader()
                                 .getMessageDocumentHeaderMetaInformation();
        var toSend = new ProducerRecord<String, Object>("validated-historical-data",
                                                        null,
                                                        info.getConnectionid(),
                                                        marketDocument,
                                                        cimToHeaders(info));
        var kafka = kafkaTemplate.send(toSend);
        awaitFuture(kafka, "Could not produce validated historical data market document message");
        LOGGER.debug("Produced validated historical data market document message for permission request {}",
                     info.getPermissionid());
    }

    private static Iterable<Header> cimToHeaders(energy.eddie.cim.v0_82.vhd.MessageDocumentHeaderMetaInformationComplexType header) {
        return List.of(
                new StringHeader(PERMISSION_ID, header.getPermissionid()),
                new StringHeader(CONNECTION_ID, header.getConnectionid()),
                new StringHeader(DATA_NEED_ID, header.getDataNeedid())
        );
    }

    private void produceRawDataMessage(RawDataMessage message) {
        var toSend = new ProducerRecord<String, Object>("raw-data-in-proprietary-format",
                                                        message.connectionId(),
                                                        message);
        var future = kafkaTemplate.send(toSend);
        awaitFuture(future, "Could not produce raw data message");
        LOGGER.debug("Produced raw data message for permission request {}", message.permissionId());
    }

    private void produceAccountingPointEnvelope(
            AccountingPointEnvelope marketDocument
    ) {
        var header = marketDocument.getMessageDocumentHeader()
                                   .getMessageDocumentHeaderMetaInformation();
        var toSend = new ProducerRecord<String, Object>(
                "accounting-point-market-documents",
                null,
                header.getConnectionid(),
                marketDocument,
                cimToHeaders(header)
        );
        var future = kafkaTemplate.send(toSend);
        awaitFuture(future, "Could not produce accounting point market document message");
        LOGGER.debug("Produced accounting point market document message for permission request {}",
                     header.getPermissionid());
    }

    private static List<Header> cimToHeaders(
            MessageDocumentHeaderMetaInformationComplexType header
    ) {
        return List.of(
                new StringHeader(PERMISSION_ID, header.getPermissionid()),
                new StringHeader(CONNECTION_ID, header.getConnectionid()),
                new StringHeader(DATA_NEED_ID, header.getDataNeedid())
        );
    }

    private record StringHeader(String key, String payload) implements Header {
        @Override
        public byte[] value() {
            return payload.getBytes(StandardCharsets.UTF_8);
        }
    }
}
