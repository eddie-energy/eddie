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
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InboundAcknowledgementPublisherTest {
    private static final String ACK_TOPIC_PREFIX = "ack/topic";
    private static final UUID AIIDA_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Captor
    private ArgumentCaptor<byte[]> payloadCaptor;

    @Mock
    private IMqttAsyncClient mqttClient;
    @Mock
    private AckFormatterStrategyRegistry ackFormatterStrategyRegistry;
    @Mock
    private AckFormatterStrategy ackFormatterStrategy;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        var builder = JsonMapper.builder();
        new AiidaConfiguration().objectMapperCustomizer().customize(builder);
        objectMapper = builder.build();
    }

    @Test
    void publishAcknowledgement_publishesMessage() throws Exception {
        // Given
        var expectedTopic = AiidaSchema.ACKNOWLEDGEMENT_CIM_V1_12.buildTopicPath(ACK_TOPIC_PREFIX);
        var inboundAckStreamer = new InboundAcknowledgementPublisher(
                AIIDA_ID,
                objectMapper,
                ACK_TOPIC_PREFIX,
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

        inboundAckStreamer.setMqttClient(mqttClient);

        // When
        inboundAckStreamer.publishAcknowledgement(inboundRecord);

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

        var inboundAckStreamer = new InboundAcknowledgementPublisher(
                AIIDA_ID,
                objectMapper,
                ACK_TOPIC_PREFIX,
                ackFormatterStrategyRegistry
        );

        // When
        inboundAckStreamer.setMqttClient(mqttClient);
        inboundAckStreamer.publishAcknowledgement(inboundRecord);

        // Then
        verify(mqttClient, never()).publish(anyString(), any(), any(), any());
    }

    @Test
    void publishAcknowledgement_doesNotPublish_withoutMqttClient() throws Exception {
        // Given
        var inboundRecord = mock(InboundRecord.class);
        var inboundAckStreamer = new InboundAcknowledgementPublisher(AIIDA_ID, objectMapper, ACK_TOPIC_PREFIX);

        // When
        inboundAckStreamer.publishAcknowledgement(inboundRecord);

        // Then
        verify(mqttClient, never()).publish(anyString(), any(), anyInt(), anyBoolean());
    }

    @Test
    void publishAcknowledgement_doesNotPublish_withoutAckTopic() throws Exception {
        // Given
        var inboundRecord = mock(InboundRecord.class);
        var inboundAckStreamer = new InboundAcknowledgementPublisher(AIIDA_ID, objectMapper, null);
        inboundAckStreamer.setMqttClient(mqttClient);

        // When
        inboundAckStreamer.publishAcknowledgement(inboundRecord);

        // Then
        verify(mqttClient, never()).publish(anyString(), any(), anyInt(), anyBoolean());
    }
}
