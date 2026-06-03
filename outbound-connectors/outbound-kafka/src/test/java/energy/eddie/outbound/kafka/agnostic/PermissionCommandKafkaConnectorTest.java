// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.kafka.agnostic;

import energy.eddie.api.agnostic.outbound.PermissionCommandOutboundConnector;
import energy.eddie.cim.agnostic.PermissionCommand;
import energy.eddie.outbound.kafka.KafkaTestConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("SpringBootApplicationProperties")
@SpringBootTest(classes = {PermissionCommandKafkaConnector.class, KafkaTestConfig.class}, properties = {"outbound-connector.kafka.eddie-id=eddie"})
@EnableKafka
@EmbeddedKafka
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Tag("Integration")
class PermissionCommandKafkaConnectorTest {
    private static final String TOPIC = "fw.eddie.agnostic.permission-command";
    private static final UUID PERMISSION_ID = UUID.randomUUID();
    private static final PermissionCommand COMMAND = new PermissionCommand.UpdateSchedule("eddie",
                                                                                          PERMISSION_ID,
                                                                                          "0 */1 * * * *");

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired
    private KafkaTemplate<String, String> stringKafkaTemplate;
    @Autowired
    private PermissionCommandOutboundConnector permissionCommandConnector;

    @SuppressWarnings("DataFlowIssue")
    @Test
    void testGettingPermissionCommand() throws ExecutionException, InterruptedException {
        // When
        kafkaTemplate.send(new ProducerRecord<>(TOPIC, "id", COMMAND)).get();

        // Then
        var result = permissionCommandConnector.getPermissionCommands().blockFirst();
        assertAll(
                () -> assertNotNull(result),
                () -> assertInstanceOf(PermissionCommand.UpdateSchedule.class, result),
                () -> assertEquals(PERMISSION_ID, result.permissionId())
        );
    }

    @Test
    void testPermissionCommandWithInvalidFormat() throws ExecutionException, InterruptedException {
        // Given
        stringKafkaTemplate.send(new ProducerRecord<>(TOPIC, "id", "Invalid JSON")).get();
        kafkaTemplate.send(new ProducerRecord<>(TOPIC, "id", COMMAND)).get();

        // When
        var result = permissionCommandConnector.getPermissionCommands().blockFirst();

        // Then
        assertNotNull(result);
    }
}