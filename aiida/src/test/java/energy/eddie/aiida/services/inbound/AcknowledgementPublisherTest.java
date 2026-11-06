// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.inbound;

import energy.eddie.aiida.adapters.datasource.inbound.InboundMqttClientRegistry;
import energy.eddie.aiida.adapters.datasource.inbound.ack.AckFormatterStrategy;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.record.InboundProcessingResult;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import energy.eddie.cim.v1_12.ack.AcknowledgementEnvelope;
import org.eclipse.paho.mqttv5.client.IMqttAsyncClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcknowledgementPublisherTest {
    private static final UUID DATA_SOURCE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Mock
    private InboundMqttClientRegistry mqttClientRegistry;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private AckFormatterStrategy formatter;
    @Mock
    private IMqttAsyncClient mqttClient;
    @Mock
    private InboundDataSource dataSource;

    @Test
    void publish_noAcknowledgementTopic_skipsPublishing() {
        var inboundRecord = mock(InboundRecord.class);
        var processingResult = InboundProcessingResult.accepted();
        when(inboundRecord.dataSource()).thenReturn(dataSource);
        when(dataSource.acknowledgementTopic()).thenReturn(null);

        publisher(List.of()).publish(inboundRecord, processingResult);

        verifyNoInteractions(mqttClientRegistry);
        verifyNoInteractions(formatter);
    }

    @Test
    void publish_noFormatterForSchema_skipsPublishing() {
        var inboundRecord = mock(InboundRecord.class);
        var processingResult = InboundProcessingResult.accepted();
        when(inboundRecord.dataSource()).thenReturn(dataSource);
        when(dataSource.acknowledgementTopic()).thenReturn("ack/topic");
        when(inboundRecord.schema()).thenReturn(AiidaSchema.OPAQUE);

        publisher(List.of()).publish(inboundRecord, processingResult);

        verifyNoInteractions(mqttClientRegistry);
    }

    @Test
    void publish_noMqttClient_skipsPublishing() {
        var inboundRecord = mock(InboundRecord.class);
        var processingResult = InboundProcessingResult.accepted();
        when(formatter.supportedSchema()).thenReturn(AiidaSchema.OPAQUE);
        when(inboundRecord.dataSource()).thenReturn(dataSource);
        when(dataSource.acknowledgementTopic()).thenReturn("ack/topic");
        when(inboundRecord.schema()).thenReturn(AiidaSchema.OPAQUE);
        when(dataSource.id()).thenReturn(DATA_SOURCE_ID);
        when(mqttClientRegistry.clientFor(DATA_SOURCE_ID)).thenReturn(Optional.empty());

        publisher(List.of(formatter)).publish(inboundRecord, processingResult);

        verifyNoInteractions(objectMapper);
        verifyNoInteractions(mqttClient);
    }

    @Test
    void publish_publishesAcknowledgement() throws Exception {
        var inboundRecord = mock(InboundRecord.class);
        var processingResult = InboundProcessingResult.accepted();
        var acknowledgementEnvelope = new AcknowledgementEnvelope();
        when(formatter.supportedSchema()).thenReturn(AiidaSchema.OPAQUE);
        when(inboundRecord.dataSource()).thenReturn(dataSource);
        when(dataSource.acknowledgementTopic()).thenReturn("ack/topic");
        when(inboundRecord.schema()).thenReturn(AiidaSchema.OPAQUE);
        when(dataSource.id()).thenReturn(DATA_SOURCE_ID);
        when(mqttClientRegistry.clientFor(DATA_SOURCE_ID)).thenReturn(Optional.of(mqttClient));
        when(formatter.convert(inboundRecord, processingResult)).thenReturn(acknowledgementEnvelope);
        when(objectMapper.writeValueAsBytes(acknowledgementEnvelope)).thenReturn(new byte[]{1, 2, 3});

        publisher(List.of(formatter)).publish(inboundRecord, processingResult);

        verify(formatter).convert(inboundRecord, processingResult);
        verify(mqttClient).publish(eq("ack/topic/acknowledgement-cim-v1-12"), aryEq(new byte[]{1, 2, 3}), eq(0), eq(false));
    }

    @Test
    void constructor_rejectsMultipleFormattersForOneSchema() {
        var duplicate = mock(AckFormatterStrategy.class);
        when(formatter.supportedSchema()).thenReturn(AiidaSchema.OPAQUE);
        when(duplicate.supportedSchema()).thenReturn(AiidaSchema.OPAQUE);

        assertThrows(IllegalStateException.class, () -> publisher(List.of(formatter, duplicate)));
    }

    private AcknowledgementPublisher publisher(List<AckFormatterStrategy> formatters) {
        return new AcknowledgementPublisher(mqttClientRegistry, objectMapper, formatters);
    }
}
