package energy.eddie.aiida.streamers.kafka;

import energy.eddie.aiida.models.permission.KafkaStreamingConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

public class KafkaProducerFactory {
    private KafkaProducerFactory() {
    }

    /**
     * Creates a new {@link KafkaProducer} that uses a {@link StringSerializer} to serialize key and value.
     * for the values.
     * Sets the <i>client.id</i> of the Producer to {@code connectionId} and the <i>bootstrap.servers</i>
     * to the value of {@code streamingConfig.bootstrapServers()}.
     *
     * @param streamingConfig {@link KafkaStreamingConfig} object with config that should be applied to the returned KafkaProducer.
     * @param connectionId    The string that should be used as <i>client.id</i>
     * @return A KafkaProducer instance with the default config applied to it.
     */
    public static Producer<String, String> getKafkaProducer(KafkaStreamingConfig streamingConfig, String connectionId) {
        Properties properties = new Properties();
        properties.put("client.id", connectionId);
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        properties.put(ProducerConfig.LINGER_MS_CONFIG, "1");
        properties.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "none");
        properties.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, "15000");
        properties.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, "5000");
        properties.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, "5000");
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, streamingConfig.bootstrapServers());

        return new KafkaProducer<>(properties, new StringSerializer(), new StringSerializer());
    }
}
