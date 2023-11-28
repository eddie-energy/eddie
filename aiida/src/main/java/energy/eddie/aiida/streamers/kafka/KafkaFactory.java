package energy.eddie.aiida.streamers.kafka;

import energy.eddie.aiida.models.permission.KafkaStreamingConfig;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Duration;
import java.util.Properties;

public class KafkaFactory {
    private KafkaFactory() {
    }

    /**
     * Creates a new {@link KafkaProducer} that uses a {@link StringSerializer} to serialize key and value.
     * for the values.
     * Sets the <i>client.id</i> of the Producer to {@code permissionId} and the <i>bootstrap.servers</i>
     * to the value of {@code streamingConfig.bootstrapServers()}.
     *
     * @param streamingConfig {@link KafkaStreamingConfig} object with config that should be applied to the returned KafkaProducer.
     * @param permissionId    The string that should be used as <i>client.id</i>
     * @return A KafkaProducer instance with the default config applied to it.
     */
    public static Producer<String, String> getKafkaProducer(KafkaStreamingConfig streamingConfig, String permissionId) {
        Properties properties = new Properties();
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, permissionId);
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        properties.put(ProducerConfig.LINGER_MS_CONFIG, "1");
        properties.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "none");
        properties.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, "15000");
        properties.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, "5000");
        properties.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, "5000");
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, streamingConfig.bootstrapServers());

        return new KafkaProducer<>(properties, new StringSerializer(), new StringSerializer());
    }

    public static Consumer<String, String> getKafkaConsumer(KafkaStreamingConfig streamingConfig, String permissionId, Duration terminationRequestPollInterval) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.CLIENT_ID_CONFIG, permissionId);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, permissionId);
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        properties.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, "5000");
        // Kafka server should not consider the client dead if it doesn't poll for a long time.
        // The poll loop is simple, therefore we should not miss any intervals, *3 is generous
        properties.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, (int) terminationRequestPollInterval.toMillis() * 3);
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, streamingConfig.bootstrapServers());

        return new KafkaConsumer<>(properties, new StringDeserializer(), new StringDeserializer());
    }
}
