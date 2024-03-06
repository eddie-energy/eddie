package energy.eddie.outbound.kafka;

import energy.eddie.api.utils.Pair;
import energy.eddie.api.v0_82.outbound.TerminationConnector;
import energy.eddie.cim.v0_82.cmd.ConsentMarketDocument;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.receiver.ReceiverRecord;

import java.util.Collections;
import java.util.Properties;

import static org.apache.kafka.common.requests.FetchMetadata.log;

public class TerminationKafkaConnector implements TerminationConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(TerminationKafkaConnector.class);
    private final Flux<Pair<String, ConsentMarketDocument>> flux;

    public TerminationKafkaConnector(Properties kafkaProperties, String terminationTopic) {
        LOGGER.info("Creating TerminationKafkaConnector which will listen on topic {}", terminationTopic);
        kafkaProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "termination-group");
        ReceiverOptions<String, ConsentMarketDocument> options = ReceiverOptions
                .<String, ConsentMarketDocument>create(kafkaProperties)
                .subscription(Collections.singleton(terminationTopic))
                // ensure no messages are skipped: start at the beginning of topic of no committed offsets are found
                .consumerProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
                .commitBatchSize(1) // commit after every message
                .withKeyDeserializer(new StringDeserializer())
                .withValueDeserializer(new CustomDeserializer())
                .addAssignListener(partitions -> log.debug("onPartitionsAssigned {}", partitions))
                .addRevokeListener(partitions -> log.debug("onPartitionsRevoked {}", partitions));

        flux = KafkaReceiver.create(options)
                .receive()
                .map(this::process)
                .retry();
    }

    private Pair<String, ConsentMarketDocument> process(ReceiverRecord<String, ConsentMarketDocument> rec) {
        LOGGER.debug("Got new ConsentMarketDocument {}", rec);
        rec.receiverOffset().acknowledge();
        return new Pair<>(rec.key(), rec.value());
    }

    @Override
    public Flux<Pair<String, ConsentMarketDocument>> getTerminationMessages() {
        return flux;
    }
}
