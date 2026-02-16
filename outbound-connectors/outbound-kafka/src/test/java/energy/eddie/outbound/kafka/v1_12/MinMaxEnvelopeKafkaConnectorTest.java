// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.kafka.v1_12;

import energy.eddie.api.v1_12.outbound.MinMaxEnvelopeOutboundConnector;
import energy.eddie.cim.v1_12.recmmoe.RECMMOEEnvelope;
import energy.eddie.cim.v1_12.recmmoe.RECMMOEMarketDocument;
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

import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("SpringBootApplicationProperties")
@SpringBootTest(classes = {MinMaxEnvelopeKafkaConnector.class, KafkaTestConfig.class}, properties = {"outbound-connector.kafka.eddie-id=eddie"})
@EnableKafka
@EmbeddedKafka
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Tag("Integration")
class MinMaxEnvelopeKafkaConnectorTest {
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired
    private KafkaTemplate<String, String> stringKafkaTemplate;
    @Autowired
    private MinMaxEnvelopeOutboundConnector minMaxConnector;

    @SuppressWarnings("DataFlowIssue")
    @Test
    void testGettingMinMaxEnvelope() throws ExecutionException, InterruptedException {
        // Given
        var envelope = new RECMMOEEnvelope()
                .withMarketDocument(new RECMMOEMarketDocument().withMRID("some-id"));
        // When
        kafkaTemplate.send(new ProducerRecord<>("fw.eddie.cim_1_12.min-max-envelope-md", "id", envelope)).get();

        // Then
        var result = minMaxConnector.getMinMaxEnvelopes().blockFirst();
        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(envelope.getMarketDocument().getMRID(), result.getMarketDocument().getMRID())
        );
    }

    @Test
    void testMinMaxEnvelopeWithInvalidFormat() throws ExecutionException, InterruptedException {
        // Given
        stringKafkaTemplate.send(new ProducerRecord<>("fw.eddie.cim_1_12.min-max-envelope-md", "id", "Invalid JSON"))
                           .get();
        var envelope = new RECMMOEEnvelope()
                .withMarketDocument(new RECMMOEMarketDocument().withMRID("some-id"));
        kafkaTemplate.send(new ProducerRecord<>("fw.eddie.cim_1_12.min-max-envelope-md", "id", envelope)).get();

        // When
        var result = minMaxConnector.getMinMaxEnvelopes()
                                    .blockFirst();

        // Then
        assertNotNull(result);
    }
}