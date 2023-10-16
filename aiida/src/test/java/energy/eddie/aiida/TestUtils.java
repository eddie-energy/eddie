package energy.eddie.aiida;

import energy.eddie.aiida.models.permission.KafkaStreamingConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.TestInfo;
import org.testcontainers.containers.KafkaContainer;

import java.util.Properties;

public class TestUtils {
    /**
     * Creates a KafkaConsumer that connects to the testcontainer of this testclass and uses the displayName of
     * the supplied testInfo as <i>group.id</i>.
     */
    public static KafkaConsumer<String, String> getKafkaConsumer(TestInfo testInfo, KafkaContainer kafka) {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", kafka.getBootstrapServers());
        properties.put("group.id", testInfo.getDisplayName());
        // make sure to consume all records even if consumer subscribed after records have already been published
        properties.put("auto.offset.reset", "earliest");
        return new KafkaConsumer<>(properties, new StringDeserializer(), new StringDeserializer());
    }

    /**
     * Creates a KafkaStreamingConfig with unique topic names allowing that the KafkaContainer can be shared between
     * tests and to ensure no test method reuses the topic of another method.
     *
     * @param testInfo testInfo object of the test method
     */
    public static KafkaStreamingConfig getKafkaConfig(TestInfo testInfo, KafkaContainer kafka) {
        String prefix = testInfo.getDisplayName().substring(0, testInfo.getDisplayName().indexOf("("));
        var dataTopic = prefix + "_data";
        var statusTopic = prefix + "_status";
        var subscribeTopic = prefix + "_subscribe";
        return new KafkaStreamingConfig(kafka.getBootstrapServers(), dataTopic, statusTopic, subscribeTopic);
    }
}
