package energy.eddie.outbound.kafka;

import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.cim.v0_82.pmd.PermissionMarketDocumentComplexType;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.DockerImageName;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

@Tag("Integration")
class TerminationKafkaConnectorTest {
    public static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"))
            .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true")
            .withKraft();

    @BeforeAll
    static void setup() {
        kafka.start();
    }

    @AfterAll
    static void teardown() {
        kafka.stop();
    }

    @Test
    void testGettingTermination() throws ExecutionException, InterruptedException {
        // Given
        Producer<String, Object> producer = new KafkaProducer<>(createProducerConfig(), new StringSerializer(),
                                                                new CustomSerializer());
        var props = createProducerConfig();
        props.put("auto.offset.reset", "earliest");
        var terminationConnector = new TerminationKafkaConnector(props, "termination-topic");
        var pmd = new PermissionMarketDocumentComplexType().withMRID("permissionId");
        var envelope = new PermissionEnvelope().withPermissionMarketDocument(pmd);
        // When
        producer.send(
                        new ProducerRecord<>("termination-topic", "id", envelope))
                .get();

        // Then
        var pair = terminationConnector.getTerminationMessages()
                .blockFirst();
        assertAll(
                () -> assertNotNull(pair),
                () -> assertEquals("id", pair.key()),
                () -> assertEquals("permissionId", pair.value().getPermissionMarketDocument().getMRID())
        );

        // Clean-Up
        producer.close();
    }

    @Test
    void testTerminationWithInvalidFormat() throws ExecutionException, InterruptedException, JsonProcessingException {
        // Given
        Producer<String, String> producer = new KafkaProducer<>(createProducerConfig(), new StringSerializer(),
                                                                new StringSerializer());
        var props = createProducerConfig();
        props.put("auto.offset.reset", "earliest");
        var terminationConnector = new TerminationKafkaConnector(props, "termination-topic");
        ObjectMapper mapper = new ObjectMapper();

        // When
        producer.send(new ProducerRecord<>("termination-topic", "id", "INVALID JSON")).get();
        var pmd = new PermissionMarketDocumentComplexType().withMRID("permissionId");
        var envelope = new PermissionEnvelope().withPermissionMarketDocument(pmd);
        producer.send(new ProducerRecord<>("termination-topic", "id", mapper.writeValueAsString(envelope))).get();
        var pair = terminationConnector.getTerminationMessages()
                .blockFirst();

        // Then
        assertNotNull(pair);

        // Clean-Up
        producer.close();
    }

    private Properties createProducerConfig() {
        Properties properties = new Properties();
        String bootstrapServers = kafka.getBootstrapServers();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return properties;
    }
}