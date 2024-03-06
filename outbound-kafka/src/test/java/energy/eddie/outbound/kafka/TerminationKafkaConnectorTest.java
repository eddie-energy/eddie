package energy.eddie.outbound.kafka;

import energy.eddie.cim.v0_82.cmd.ConsentMarketDocument;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

@Tag("Integration")
class TerminationKafkaConnectorTest {
    public static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"))
            .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true")
            .withKraft();

    @Test
    void testGettingTermination() throws ExecutionException, InterruptedException {
        // Given
        kafka.start();

        Producer<String, Object> producer = new KafkaProducer<>(createProducerConfig(), new StringSerializer(), new CustomSerializer());
        var props = createProducerConfig();
        props.put("auto.offset.reset", "earliest");
        var terminationConnector = new TerminationKafkaConnector(props, "termination-topic");

        // When
        producer.send(new ProducerRecord<>("termination-topic", "id", new ConsentMarketDocument().withMRID("permissionId"))).get();

        // Then
        var pair = terminationConnector.getTerminationMessages()
                .blockFirst();
        assertAll(
                () -> assertNotNull(pair),
                () -> assertEquals("id", pair.key()),
                () -> assertEquals("permissionId", pair.value().getMRID())
        );

        // Clean-Up
        producer.close();
        kafka.stop();
    }

    private Properties createProducerConfig() {
        Properties properties = new Properties();
        String bootstrapServers = kafka.getBootstrapServers();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return properties;
    }
}