// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.kafka.agnostic;

import energy.eddie.api.agnostic.opaque.OpaqueEnvelope;
import energy.eddie.api.agnostic.outbound.OpaqueEnvelopeOutboundConnector;
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

import java.time.ZonedDateTime;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("SpringBootApplicationProperties")
@SpringBootTest(classes = {OpaqueEnvelopeKafkaConnector.class, KafkaTestConfig.class}, properties = {"outbound-connector.kafka.eddie-id=eddie"})
@EnableKafka
@EmbeddedKafka
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Tag("Integration")
class OpaqueEnvelopeKafkaConnectorTest {
    private static final String ID = "test-id";
    private static final OpaqueEnvelope ENVELOPE = new OpaqueEnvelope(ID,
                                                                      ID,
                                                                      ID,
                                                                      ID,
                                                                      ID,
                                                                      ZonedDateTime.now(),
                                                                      "test-payload");

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired
    private KafkaTemplate<String, String> stringKafkaTemplate;
    @Autowired
    private OpaqueEnvelopeOutboundConnector opaqueConnector;

    @SuppressWarnings("DataFlowIssue")
    @Test
    void testGettingOpaqueEnvelope() throws ExecutionException, InterruptedException {
        // When
        kafkaTemplate.send(new ProducerRecord<>("fw.eddie.agnostic.opaque-envelope", "id", ENVELOPE)).get();

        // Then
        var result = opaqueConnector.getOpaqueEnvelopes().blockFirst();
        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(ID, result.messageId())
        );
    }

    @Test
    void testOpaqueEnvelopeWithInvalidFormat() throws ExecutionException, InterruptedException {
        // Given
        stringKafkaTemplate.send(new ProducerRecord<>("fw.eddie.agnostic.opaque-envelope", "id", "Invalid JSON"))
                           .get();
        kafkaTemplate.send(new ProducerRecord<>("fw.eddie.agnostic.opaque-envelope", "id", ENVELOPE)).get();

        // When
        var result = opaqueConnector.getOpaqueEnvelopes()
                                    .blockFirst();

        // Then
        assertNotNull(result);
    }
}