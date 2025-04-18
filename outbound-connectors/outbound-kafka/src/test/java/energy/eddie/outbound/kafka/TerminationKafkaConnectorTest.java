package energy.eddie.outbound.kafka;

import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.cim.v0_82.pmd.PermissionMarketDocumentComplexType;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {TerminationKafkaConnector.class, KafkaTestConfig.class}, properties = {"outbound-connector.kafka.eddie-id=eddie"})
@EnableKafka
@EmbeddedKafka
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Tag("Integration")
class TerminationKafkaConnectorTest {
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired
    private KafkaTemplate<String, String> stringKafkaTemplate;
    @Autowired
    private TerminationKafkaConnector terminationConnector;

    @SuppressWarnings("DataFlowIssue")
    @Test
    void testGettingTermination() throws ExecutionException, InterruptedException {
        // Given
        var pmd = new PermissionMarketDocumentComplexType().withMRID("permissionId");
        var envelope = new PermissionEnvelope().withPermissionMarketDocument(pmd);
        // When
        kafkaTemplate.send(new ProducerRecord<>("fw.eddie.cim_0_82.termination-md", "id", envelope)).get();

        // Then
        var pair = terminationConnector.getTerminationMessages().blockFirst();
        assertAll(
                () -> assertNotNull(pair),
                () -> assertEquals("id", pair.key()),
                () -> assertEquals("permissionId", pair.value().getPermissionMarketDocument().getMRID())
        );
    }

    @Test
    void testTerminationWithInvalidFormat() throws ExecutionException, InterruptedException {
        // Given
        stringKafkaTemplate.send(new ProducerRecord<>("fw.eddie.cim_0_82.termination-md", "id", "Invalid JSON")).get();
        kafkaTemplate.send(new ProducerRecord<>("fw.eddie.cim_0_82.termination-md", "id", new PermissionEnvelope())).get();

        // When
        var pair = terminationConnector.getTerminationMessages()
                                       .blockFirst();

        // Then
        assertNotNull(pair);
    }
}