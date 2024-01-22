package energy.eddie.outbound.kafka;

import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.api.agnostic.RawDataOutboundConnector;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.api.v0.Mvp1ConnectionStatusMessageOutboundConnector;
import energy.eddie.api.v0.Mvp1ConsumptionRecordOutboundConnector;
import energy.eddie.api.v0_82.EddieValidatedHistoricalDataMarketDocumentOutboundConnector;
import energy.eddie.api.v0_82.cim.EddieValidatedHistoricalDataMarketDocument;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.adapter.JdkFlowAdapter;

import java.io.Closeable;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;

public class KafkaConnector implements Mvp1ConnectionStatusMessageOutboundConnector,
        Mvp1ConsumptionRecordOutboundConnector, EddieValidatedHistoricalDataMarketDocumentOutboundConnector, Closeable,
        RawDataOutboundConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaConnector.class);
    private final KafkaProducer<String, Object> kafkaProducer;

    public KafkaConnector(Properties kafkaProperties) {
        kafkaProducer = new KafkaProducer<>(kafkaProperties, new StringSerializer(), new CustomSerializer());
    }

    @Override
    public void setConnectionStatusMessageStream(Flow.Publisher<ConnectionStatusMessage> statusMessageStream) {
        JdkFlowAdapter
                .flowPublisherToFlux(statusMessageStream)
                .subscribe(this::produceStatusMessage);
    }

    @Override
    public void setEddieValidatedHistoricalDataMarketDocumentStream(Flow.Publisher<EddieValidatedHistoricalDataMarketDocument> marketDocumentStream) {
        JdkFlowAdapter
                .flowPublisherToFlux(marketDocumentStream)
                .subscribe(this::produceEddieValidatedHistoricalDataMarketDocument);
    }

    @Override
    public void setConsumptionRecordStream(Flow.Publisher<ConsumptionRecord> consumptionRecordStream) {
        JdkFlowAdapter
                .flowPublisherToFlux(consumptionRecordStream)
                .subscribe(this::produceConsumptionRecord);
    }

    @Override
    public void close() {
        kafkaProducer.close();
    }

    private void produceStatusMessage(ConnectionStatusMessage statusMessage) {
        try {
            kafkaProducer
                    .send(new ProducerRecord<>("status-messages", statusMessage))
                    .get();
            LOGGER.info("Produced connection status message");
        } catch (RuntimeException | ExecutionException e) {
            LOGGER.warn("Could not produce connection status message", e);
        } catch (InterruptedException e) {
            reinterrupt(e);
        }
    }

    private void produceConsumptionRecord(ConsumptionRecord consumptionRecord) {
        try {
            kafkaProducer
                    .send(new ProducerRecord<>("consumption-records", consumptionRecord.getConnectionId(), consumptionRecord))
                    .get();
            LOGGER.info("Produced consumption record message");
        } catch (RuntimeException | ExecutionException e) {
            LOGGER.warn("Could not produce consumption record message", e);
        } catch (InterruptedException e) {
            reinterrupt(e);
        }
    }

    private void produceEddieValidatedHistoricalDataMarketDocument(EddieValidatedHistoricalDataMarketDocument marketDocument) {
        try {
            kafkaProducer
                    .send(new ProducerRecord<>("validated-historical-data", marketDocument.connectionId().orElse(null), marketDocument))
                    .get();
            LOGGER.info("Produced validated historical data market document message");
        } catch (RuntimeException | ExecutionException e) {
            LOGGER.warn("Could not produce validated historical data market document message", e);
        } catch (InterruptedException e) {
            reinterrupt(e);
        }
    }

    private void reinterrupt(InterruptedException e) {
        LOGGER.warn("Thread was interrupted", e);
        Thread.currentThread().interrupt();
    }

    @Override
    public void setRawDataStream(Flow.Publisher<RawDataMessage> rawDataStream) {
        JdkFlowAdapter
                .flowPublisherToFlux(rawDataStream)
                .subscribe(this::produceRawDataMessage);
    }

    private void produceRawDataMessage(RawDataMessage message) {
        try {
            kafkaProducer
                    .send(new ProducerRecord<>("raw-data-in-proprietary-format", message.connectionId(), message))
                    .get();
            LOGGER.debug("Produced raw data message");
        } catch (RuntimeException | ExecutionException e) {
            LOGGER.warn("Could not produce raw data message", e);
        } catch (InterruptedException e) {
            reinterrupt(e);
        }
    }
}