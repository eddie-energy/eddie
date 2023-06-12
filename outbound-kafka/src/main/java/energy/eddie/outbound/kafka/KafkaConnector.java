package energy.eddie.outbound.kafka;

import energy.eddie.api.v0.ApplicationConnector;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.ConsumptionRecord;
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

public class KafkaConnector implements ApplicationConnector, Closeable {
    private final Logger logger = LoggerFactory.getLogger(KafkaConnector.class);
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
    public void setConsumptionRecordStream(Flow.Publisher<ConsumptionRecord> consumptionRecordStream) {
        JdkFlowAdapter
                .flowPublisherToFlux(consumptionRecordStream)
                .subscribe(this::produceConsumptionRecord);
    }

    @Override
    public void init() {
        // To be removed
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
            logger.info("Produced connection status message");
        } catch (RuntimeException | ExecutionException e) {
            logger.warn("Could not produce connection status message", e);
        } catch (InterruptedException e) {
            reinterrupt(e);
        }
    }

    private void produceConsumptionRecord(ConsumptionRecord consumptionRecord) {
        try {
            kafkaProducer
                    .send(new ProducerRecord<>("consumption-records", consumptionRecord.getConnectionId(), consumptionRecord))
                    .get();
            logger.info("Produced consumption record message");
        } catch (RuntimeException | ExecutionException e) {
            logger.warn("Could not produce consumption record message", e);
        } catch (InterruptedException e) {
            reinterrupt(e);
        }
    }

    private void reinterrupt(InterruptedException e) {
        logger.warn("Thread was interrupted", e);
        Thread.currentThread().interrupt();
    }
}
