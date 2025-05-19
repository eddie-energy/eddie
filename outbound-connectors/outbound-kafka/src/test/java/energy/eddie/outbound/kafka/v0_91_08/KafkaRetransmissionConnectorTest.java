package energy.eddie.outbound.kafka.v0_91_08;

import energy.eddie.api.agnostic.outbound.RetransmissionOutboundConnector;
import energy.eddie.cim.v0_91_08.ESMPDateTimeInterval;
import energy.eddie.cim.v0_91_08.RTREnvelope;
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

import java.time.LocalDate;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("SpringBootApplicationProperties")
@SpringBootTest(classes = {KafkaRetransmissionConnector.class, KafkaTestConfig.class}, properties = "outbound-connector.kafka.eddie-id=eddie")
@EnableKafka
@EmbeddedKafka
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Tag("Integration")
class KafkaRetransmissionConnectorTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired
    private KafkaTemplate<String, String> stringKafkaTemplate;
    @Autowired
    private RetransmissionOutboundConnector retransmissionConnector;

    @SuppressWarnings("DataFlowIssue")
    @Test
    void testGettingRetransmissionRequest() throws ExecutionException, InterruptedException {
        // Given
        var envelope = new RTREnvelope()
                .withMessageDocumentHeaderMetaInformationPermissionId("permissionId")
                .withMessageDocumentHeaderMetaInformationRegionConnector("rc-id")
                .withMarketDocumentPeriodTimeInterval(
                        new ESMPDateTimeInterval()
                                .withStart("2025-01-01T01:01Z")
                                .withEnd("2025-01-02T00:00Z")
                );
        // When
        kafkaTemplate.send(new ProducerRecord<>("fw.eddie.cim_0_91_08.redistribution-transaction-rd", "id", envelope)).get();

        // Then
        var request = retransmissionConnector.retransmissionRequests().blockFirst();
        assertAll(
                () -> assertEquals("permissionId", request.permissionId()),
                () -> assertEquals("rc-id", request.regionConnectorId()),
                () -> assertEquals(LocalDate.of(2025, 1, 1), request.from()),
                () -> assertEquals(LocalDate.of(2025, 1, 2), request.to())
        );
    }

    @Test
    void testRetransmissionRequestWithInvalidFormat() throws ExecutionException, InterruptedException {
        // Given
        stringKafkaTemplate.send(new ProducerRecord<>("fw.eddie.cim_0_91_08.redistribution-transaction-rd", "id", "Invalid JSON")).get();
        var envelope = new RTREnvelope()
                .withMessageDocumentHeaderMetaInformationPermissionId("permissionId")
                .withMessageDocumentHeaderMetaInformationRegionConnector("rc-id")
                .withMarketDocumentPeriodTimeInterval(
                        new ESMPDateTimeInterval()
                                .withStart("2025-01-01T01:01Z")
                                .withEnd("2025-01-02T00:00Z")
                );
        kafkaTemplate.send(new ProducerRecord<>("fw.eddie.cim_0_91_08.redistribution-transaction-rd", "id", envelope)).get();

        // When
        var request = retransmissionConnector.retransmissionRequests()
                                             .blockFirst();

        // Then
        assertNotNull(request);
    }
}