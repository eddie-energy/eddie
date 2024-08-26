package energy.eddie.outbound.kafka;

import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.api.agnostic.RawDataOutboundConnector;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.api.v0.Mvp1ConnectionStatusMessageOutboundConnector;
import energy.eddie.api.v0.Mvp1ConsumptionRecordOutboundConnector;
import energy.eddie.api.v0_82.EddieAccountingPointMarketDocumentOutboundConnector;
import energy.eddie.api.v0_82.PermissionMarketDocumentOutboundConnector;
import energy.eddie.api.v0_82.ValidatedHistoricalDataEnveloppeOutboundConnector;
import energy.eddie.api.v0_82.cim.EddieAccountingPointMarketDocument;
import energy.eddie.cim.v0_82.pmd.PermissionEnveloppe;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnveloppe;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.Closeable;
import java.util.Properties;

public class KafkaConnector implements
        Mvp1ConnectionStatusMessageOutboundConnector,
        Mvp1ConsumptionRecordOutboundConnector,
        ValidatedHistoricalDataEnveloppeOutboundConnector,
        PermissionMarketDocumentOutboundConnector,
        RawDataOutboundConnector,
        EddieAccountingPointMarketDocumentOutboundConnector,
        Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaConnector.class);
    private final KafkaProducer<String, Object> kafkaProducer;

    public KafkaConnector(Properties kafkaProperties) {
        kafkaProducer = new KafkaProducer<>(kafkaProperties, new StringSerializer(), new CustomSerializer());
    }

    @Override
    public void setConnectionStatusMessageStream(Flux<ConnectionStatusMessage> statusMessageStream) {
        statusMessageStream
                .publishOn(Schedulers.boundedElastic())
                .subscribe(this::produceStatusMessage);
    }

    private void produceStatusMessage(ConnectionStatusMessage statusMessage) {
        kafkaProducer.send(new ProducerRecord<>("status-messages", statusMessage),
                           new KafkaCallback("Could not produce connection status message"));
        LOGGER.debug("Produced connection status {} message for permission request {}",
                     statusMessage.status(),
                     statusMessage.permissionId());
    }

    @Override
    public void setPermissionMarketDocumentStream(Flux<PermissionEnveloppe> permissionMarketDocumentStream) {
        permissionMarketDocumentStream
                .publishOn(Schedulers.boundedElastic())
                .subscribe(this::producePermissionMarketDocument);
    }

    private void producePermissionMarketDocument(PermissionEnveloppe permissionMarketDocument) {
        var permissionId = permissionMarketDocument.getMessageDocumentHeader()
                                                   .getMessageDocumentHeaderMetaInformation()
                                                   .getPermissionid();
        ProducerRecord<String, Object> toSend = new ProducerRecord<>(
                "permission-market-documents",
                permissionId,
                permissionMarketDocument
        );
        kafkaProducer.send(toSend, new KafkaCallback("Could not produce permission market document"));
    }

    @Override
    public void setEddieValidatedHistoricalDataMarketDocumentStream(
            Flux<ValidatedHistoricalDataEnveloppe> marketDocumentStream
    ) {
        marketDocumentStream
                .publishOn(Schedulers.boundedElastic())
                .subscribe(this::produceEddieValidatedHistoricalDataMarketDocument);
    }

    private void produceEddieValidatedHistoricalDataMarketDocument(
            ValidatedHistoricalDataEnveloppe marketDocument
    ) {
        var info = marketDocument.getMessageDocumentHeader()
                                 .getMessageDocumentHeaderMetaInformation();
        ProducerRecord<String, Object> toSend = new ProducerRecord<>("validated-historical-data",
                                                                     info.getConnectionid(),
                                                                     marketDocument);
        kafkaProducer.send(toSend,
                           new KafkaCallback("Could not produce validated historical data market document message"));
        LOGGER.debug("Produced validated historical data market document message for permission request {}",
                     info.getPermissionid());
    }

    @Override
    public void setConsumptionRecordStream(Flux<ConsumptionRecord> consumptionRecordStream) {
        consumptionRecordStream
                .publishOn(Schedulers.boundedElastic())
                .subscribe(this::produceConsumptionRecord);
    }

    private void produceConsumptionRecord(ConsumptionRecord consumptionRecord) {
        ProducerRecord<String, Object> toSend = new ProducerRecord<>("consumption-records",
                                                                     consumptionRecord.getConnectionId(),
                                                                     consumptionRecord);
        kafkaProducer.send(toSend, new KafkaCallback("Could not produce consumption record message"));
        LOGGER.debug("Produced consumption record message for permission request {}",
                     consumptionRecord.getPermissionId());
    }

    @Override
    public void setRawDataStream(Flux<RawDataMessage> rawDataStream) {
        rawDataStream
                .publishOn(Schedulers.boundedElastic())
                .subscribe(this::produceRawDataMessage);
    }

    private void produceRawDataMessage(RawDataMessage message) {
        ProducerRecord<String, Object> toSend = new ProducerRecord<>("raw-data-in-proprietary-format",
                                                                     message.connectionId(),
                                                                     message);
        kafkaProducer.send(toSend, new KafkaCallback("Could not produce raw data message"));
        LOGGER.debug("Produced raw data message for permission request {}", message.permissionId());
    }

    @Override
    public void close() {
        kafkaProducer.close();
    }

    @Override
    public void setEddieAccountingPointMarketDocumentStream(Flux<EddieAccountingPointMarketDocument> marketDocumentStream) {
        marketDocumentStream
                .publishOn(Schedulers.boundedElastic())
                .subscribe(this::produceEddieAccountingPointMarketDocument);
    }

    private void produceEddieAccountingPointMarketDocument(
            EddieAccountingPointMarketDocument marketDocument
    ) {
        ProducerRecord<String, Object> toSend = new ProducerRecord<>("accounting-point-market-documents",
                                                                     marketDocument.connectionId(),
                                                                     marketDocument);
        kafkaProducer.send(toSend,
                           new KafkaCallback("Could not produce accounting point market document message"));
        LOGGER.debug("Produced accounting point market document message for permission request {}",
                     marketDocument.permissionId());
    }

    private record KafkaCallback(String errorLogMessage) implements Callback {
        @Override
        public void onCompletion(RecordMetadata metadata, Exception exception) {
            if (exception != null) {
                LOGGER.error(errorLogMessage, exception);
            }
        }
    }
}
