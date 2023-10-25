package energy.eddie.aiida.streamers;

import energy.eddie.aiida.errors.ConnectionStatusMessageSendFailedException;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static energy.eddie.aiida.TestUtils.getKafkaConfig;
import static energy.eddie.aiida.TestUtils.getKafkaConsumer;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// deactivate the default behaviour, instead use testcontainer
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create"     // TODO: once AIIDA is more final, use a custom schema
})
@Testcontainers
@EnableScheduling
class StreamerManagerIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> timescale = new PostgreSQLContainer<>(
            DockerImageName.parse("timescale/timescaledb:2.11.2-pg15")
                    .asCompatibleSubstituteFor("postgres")
    );
    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.3.1"));

    @Autowired
    private StreamerManager streamerManager;

    /**
     * Tests that a KafkaStreamer is correctly created and terminated and any ConnectionStatusMessage
     * is sent to the correct topic and received by the broker.
     */
    @Test
    @Timeout(10)
    void test_createStreamer_sendStatusMessages_stopStreamer(TestInfo testInfo) throws ConnectionStatusMessageSendFailedException {
        var permission = getPermissionForTest(testInfo);
        KafkaConsumer<String, String> consumer = getKafkaConsumer(testInfo, kafka);

        var acceptedMessageTimestamp = Instant.parse("2023-09-11T22:00:00.00Z");
        var acceptedMessage = new ConnectionStatusMessage(permission.connectionId(), acceptedMessageTimestamp, PermissionStatus.ACCEPTED);
        String acceptedMessageJson = "{\"connectionId\":\"NewAiidaRandomConnectionId\",\"timestamp\":1694469600.000000000,\"status\":\"ACCEPTED\"}";
        var revokedMessage = new ConnectionStatusMessage(permission.connectionId(), acceptedMessageTimestamp.plusSeconds(100), PermissionStatus.REVOKED);
        String revokedMessageJson = "{\"connectionId\":\"NewAiidaRandomConnectionId\",\"timestamp\":1694469700.000000000,\"status\":\"REVOKED\"}";

        streamerManager.createNewStreamerForPermission(permission);

        streamerManager.sendConnectionStatusMessageForPermission(acceptedMessage, permission.permissionId());
        streamerManager.sendConnectionStatusMessageForPermission(revokedMessage, permission.permissionId());

        streamerManager.stopStreamer(permission.permissionId());


        // ensure that correct data has been sent
        consumer.subscribe(List.of(permission.kafkaStreamingConfig().statusTopic()));
        var polledRecords = new ArrayList<ConsumerRecord<String, String>>();
        while (polledRecords.size() < 2) {
            for (ConsumerRecord<String, String> received : consumer.poll(Duration.ofSeconds(1))) {
                polledRecords.add(received);
            }
        }

        assertEquals(2, polledRecords.size());

        assertEquals(acceptedMessageJson, polledRecords.get(0).value());
        assertEquals(revokedMessageJson, polledRecords.get(1).value());

        consumer.close();
    }

    private Permission getPermissionForTest(TestInfo testInfo) {
        var permissionId = "72831e2c-a01c-41b8-9db6-3f51670df7a5";
        var time = Instant.parse("2023-08-01T10:00:00.00Z");
        var expiration = time.plusSeconds(800_000);
        var serviceName = "My NewAIIDA Test Service";
        var connectionId = "NewAiidaRandomConnectionId";
        var codes = Set.of("1.8.0", "2.8.0");

        var streamingConfig = getKafkaConfig(testInfo, kafka);
        var permission = new Permission(serviceName, time, expiration, time, connectionId, codes, streamingConfig);
        // set permissionId via reflections to mimic database
        ReflectionTestUtils.setField(permission, "permissionId", permissionId);
        return permission;
    }
}