// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.inbound.ack;

import energy.eddie.aiida.config.AiidaConfiguration;
import energy.eddie.aiida.errors.formatter.CimSchemaFormatterException;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import energy.eddie.cim.v1_12.ack.AcknowledgementEnvelope;
import energy.eddie.cim.v1_12.ack.MessageDocumentHeader;
import energy.eddie.cim.v1_12.ack.MetaInformation;
import org.eclipse.paho.mqttv5.client.IMqttAsyncClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.publisher.TestPublisher;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InboundAcknowledgementStreamerTest {
    private static final String ACK_TOPIC_PREFIX = "ack/topic";
    private static final UUID AIIDA_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Captor
    private ArgumentCaptor<byte[]> payloadCaptor;

    @Mock
    private Flux<InboundRecord> mockFlux;
    @Mock
    private IMqttAsyncClient mqttClient;
    @Mock
    private AckFormatterStrategyRegistry ackFormatterStrategyRegistry;
    @Mock
    private AckFormatterStrategy ackFormatterStrategy;

    private TestPublisher<InboundRecord> publisher;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        var builder = JsonMapper.builder();
        new AiidaConfiguration().objectMapperCustomizer().customize(builder);
        objectMapper = builder.build();

        publisher = TestPublisher.create();
    }

    @SuppressWarnings("unchecked")
    @Test
    void start_subscribesToFlux() {
        // Given
        var inboundAckStreamer = new InboundAcknowledgementStreamer(AIIDA_ID, objectMapper, ACK_TOPIC_PREFIX, mockFlux);

        // When
        inboundAckStreamer.start(mqttClient);

        // Then
        verify(mockFlux).subscribe(any(Consumer.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void start_doesNotSubscribe_withoutMqttClient() {
        // Given
        var inboundAckStreamer = new InboundAcknowledgementStreamer(AIIDA_ID, objectMapper, ACK_TOPIC_PREFIX, mockFlux);

        // When
        inboundAckStreamer.start(null);

        // Then
        verify(mockFlux, never()).subscribe(any(Consumer.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void start_doesNotSubscribe_withoutAckTopic() {
        // Given
        var inboundAckStreamer = new InboundAcknowledgementStreamer(AIIDA_ID, objectMapper, null, mockFlux);

        // When
        inboundAckStreamer.start(mqttClient);

        // Then
        verify(mockFlux, never()).subscribe(any(Consumer.class));
    }

    @Test
    void publishAcknowledgement_publishesMessage() throws Exception {
        // Given
        var expectedTopic = AiidaSchema.ACKNOWLEDGEMENT_CIM_V1_12.buildTopicPath(ACK_TOPIC_PREFIX);
        var inboundAckStreamer = new InboundAcknowledgementStreamer(
                AIIDA_ID,
                objectMapper,
                ACK_TOPIC_PREFIX,
                publisher.flux(),
                ackFormatterStrategyRegistry
        );
        var inboundRecord = mock(InboundRecord.class);
        var ackEnvelope = new AcknowledgementEnvelope().withMessageDocumentHeader(
                new MessageDocumentHeader().withMetaInformation(
                        new MetaInformation().withRequestPermissionId("perm-id")
                ));

        when(inboundRecord.schema()).thenReturn(AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12);
        when(ackFormatterStrategyRegistry.strategyFor(AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12, AIIDA_ID))
                .thenReturn(ackFormatterStrategy);
        when(ackFormatterStrategy.convert(any(), any())).thenReturn(ackEnvelope);

        inboundAckStreamer.start(mqttClient);

        // When
        publisher.emit(inboundRecord);

        // Then
        verify(mqttClient).publish(eq(expectedTopic), payloadCaptor.capture(), anyInt(), anyBoolean());
        var publishedPayload = payloadCaptor.getValue();
        var deserializedAck = objectMapper.readValue(publishedPayload, AcknowledgementEnvelope.class);
        assertEquals("perm-id", deserializedAck.getMessageDocumentHeader()
                                               .getMetaInformation()
                                               .getRequestPermissionId());
    }

    @Test
    void publishAcknowledgement_doesNotPublish_whenNoFormatterStrategy() throws Exception {
        // Given
        var inboundRecord = mock(InboundRecord.class);

        when(inboundRecord.schema()).thenReturn(AiidaSchema.SMART_METER_P1_RAW);
        when(ackFormatterStrategyRegistry.strategyFor(AiidaSchema.SMART_METER_P1_RAW, AIIDA_ID))
                .thenThrow(new CimSchemaFormatterException(new IllegalArgumentException("No strategy found")));

        var inboundAckStreamer = new InboundAcknowledgementStreamer(
                AIIDA_ID,
                objectMapper,
                ACK_TOPIC_PREFIX,
                publisher.flux(),
                ackFormatterStrategyRegistry
        );

        // When
        inboundAckStreamer.start(mqttClient);
        publisher.emit(inboundRecord);

        // Then
        verify(mqttClient, never()).publish(anyString(), any(), any(), any());
    }
}
