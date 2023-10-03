package energy.eddie.aiida.streamers.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.aiida.models.permission.KafkaStreamingConfig;
import energy.eddie.aiida.models.permission.PermissionStatus;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordFactory;
import energy.eddie.aiida.streamers.ConnectionStatusMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.test.publisher.TestPublisher;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;


@Testcontainers
class KafkaStreamerIntegrationTest {
    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.3.1"));
    private String record1Json;
    private String record2Json;
    private String record3Json;
    private String record4Json;
    private List<AiidaRecord> records;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        var begin = Instant.parse("2023-10-01T10:00:00.00Z");
        var record1 = AiidaRecordFactory.createRecord("1.8.0", begin.plusSeconds(1), 10);
        record1Json = "{\"type\":\"IntegerAiidaRecord\",\"timestamp\":1696154401.000000000,\"code\":\"1.8.0\",\"value\":10}";

        var record2 = AiidaRecordFactory.createRecord("1.8.0", begin.plusSeconds(2), 20);
        record2Json = "{\"type\":\"IntegerAiidaRecord\",\"timestamp\":1696154402.000000000,\"code\":\"1.8.0\",\"value\":20}";

        var record3 = AiidaRecordFactory.createRecord("1.8.0", begin.plusSeconds(3), 30);
        record3Json = "{\"type\":\"IntegerAiidaRecord\",\"timestamp\":1696154403.000000000,\"code\":\"1.8.0\",\"value\":30}";

        var record4 = AiidaRecordFactory.createRecord("C.1.0", begin.plusSeconds(4), "Hello World");
        record4Json = "{\"type\":\"StringAiidaRecord\",\"timestamp\":1696154404.000000000,\"code\":\"C.1.0\",\"value\":\"Hello World\"}";

        records = List.of(record1, record2, record3, record4);

        mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Test
    @Timeout(5)
    void givenRandomData_kafkaStreamer_sendsDataToBroker(TestInfo testInfo) {
        var config = getKafkaConfig(testInfo);
        KafkaConsumer<String, String> consumer = getKafkaConsumer(testInfo);
        String connectionId = "IntegrationTestConnectionId";
        var producer = KafkaProducerFactory.getKafkaProducer(config, connectionId);

        TestPublisher<AiidaRecord> recordPublisher = TestPublisher.create();
        TestPublisher<ConnectionStatusMessage> statusMessagePublisher = TestPublisher.create();

        var streamer = new KafkaStreamer(producer, recordPublisher.flux(), statusMessagePublisher.flux(),
                connectionId, config, mapper);


        recordPublisher.assertNoSubscribers();
        statusMessagePublisher.assertNoSubscribers();
        streamer.connect();
        recordPublisher.assertSubscribers(1);
        statusMessagePublisher.assertSubscribers(1);

        // send four records to broker
        for (AiidaRecord record : records) {
            recordPublisher.next(record);
        }

        // use separate consumer to verify data has been written to Kafka
        consumer.subscribe(List.of(config.dataTopic()));


        // need to poll broker to get all published messages, then compare data
        var polledRecords = new ArrayList<ConsumerRecord<String, String>>();
        while (polledRecords.size() < records.size()) {
            for (ConsumerRecord<String, String> received : consumer.poll(Duration.ofSeconds(1))) {
                polledRecords.add(received);
            }
        }

        assertEquals(records.size(), polledRecords.size());

        assertEquals(record1Json, polledRecords.get(0).value());
        assertEquals(record2Json, polledRecords.get(1).value());
        assertEquals(record3Json, polledRecords.get(2).value());
        assertEquals(record4Json, polledRecords.get(3).value());

        for (ConsumerRecord<String, String> polledRecord : polledRecords) {
            assertEquals("IntegrationTestConnectionId", polledRecord.key());
        }

        streamer.close();
        consumer.close();
    }

    /**
     * Creates a KafkaConsumer that connects to the testcontainer of this testclass and uses the displayName of
     * the supplied testInfo as <i>group.id</i>.
     */
    private KafkaConsumer<String, String> getKafkaConsumer(TestInfo testInfo) {
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
    private KafkaStreamingConfig getKafkaConfig(TestInfo testInfo) {
        String prefix = testInfo.getDisplayName().substring(0, testInfo.getDisplayName().indexOf("("));
        var dataTopic = prefix + "_data";
        var statusTopic = prefix + "_status";
        var subscribeTopic = prefix + "_subscribe";
        return new KafkaStreamingConfig(kafka.getBootstrapServers(), dataTopic, statusTopic, subscribeTopic);
    }

    @Test
    @Timeout(5)
    void givenStatusMessage_kafkaStreamer_sendsDataToBroker(TestInfo testInfo) {
        var config = getKafkaConfig(testInfo);
        KafkaConsumer<String, String> consumer = getKafkaConsumer(testInfo);
        String connectionId = "StatusMessageIntegrationTestConnectionId";
        var producer = KafkaProducerFactory.getKafkaProducer(config, connectionId);

        TestPublisher<AiidaRecord> recordPublisher = TestPublisher.create();
        TestPublisher<ConnectionStatusMessage> statusMessagePublisher = TestPublisher.create();

        var streamer = new KafkaStreamer(producer, recordPublisher.flux(), statusMessagePublisher.flux(),
                connectionId, config, mapper);

        var timestamp = Instant.parse("2023-11-01T10:00:00.00Z");

        var statusMessage = new ConnectionStatusMessage(connectionId, timestamp, PermissionStatus.ACCEPTED);
        var statusMessageJson = "{\"connectionId\":\"StatusMessageIntegrationTestConnectionId\",\"timestamp\":1698832800.000000000,\"status\":\"ACCEPTED\"}";

        var statusMessage2 = new ConnectionStatusMessage(connectionId, timestamp.plusSeconds(10), PermissionStatus.REVOKED);
        var statusMessageJson2 = "{\"connectionId\":\"StatusMessageIntegrationTestConnectionId\",\"timestamp\":1698832810.000000000,\"status\":\"REVOKED\"}";


        recordPublisher.assertNoSubscribers();
        statusMessagePublisher.assertNoSubscribers();
        streamer.connect();
        recordPublisher.assertSubscribers(1);
        statusMessagePublisher.assertSubscribers(1);


        statusMessagePublisher.next(statusMessage);
        statusMessagePublisher.next(statusMessage2);


        // need to poll broker to get all published messages, then compare data
        consumer.subscribe(List.of(config.statusTopic()));
        var polledRecords = new ArrayList<ConsumerRecord<String, String>>();
        while (polledRecords.size() < 2) {
            for (ConsumerRecord<String, String> received : consumer.poll(Duration.ofSeconds(1))) {
                polledRecords.add(received);
            }
        }

        assertEquals(2, polledRecords.size());

        assertEquals(statusMessageJson, polledRecords.get(0).value());
        assertEquals(connectionId, polledRecords.get(0).key());

        assertEquals(statusMessageJson2, polledRecords.get(1).value());
        assertEquals(connectionId, polledRecords.get(1).key());

        streamer.close();
        consumer.close();
    }
}