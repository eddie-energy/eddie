// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.inbound;

import energy.eddie.aiida.adapters.datasource.inbound.InboundMqttClientRegistry;
import energy.eddie.aiida.adapters.datasource.inbound.ack.AckFormatterStrategy;
import energy.eddie.aiida.aggregator.InboundAggregator;
import energy.eddie.aiida.application.information.ApplicationInformation;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.record.InboundProcessingResult;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.aiida.services.ApplicationInformationService;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import energy.eddie.cim.v1_12.ack.AcknowledgementEnvelope;
import org.eclipse.paho.mqttv5.client.IMqttAsyncClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InboundProcessingDispatcherTest {
    private static final UUID AIIDA_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID DATA_SOURCE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Mock
    private InboundAggregator inboundAggregator;
    @Mock
    private InboundMqttClientRegistry mqttClientRegistry;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private ApplicationInformationService applicationInformationService;
    @Mock
    private InboundProcessingStrategy strategy;
    @Mock
    private AckFormatterStrategy formatter;
    @Mock
    private IMqttAsyncClient mqttClient;
    @Mock
    private InboundDataSource dataSource;

    @BeforeEach
    void setUp() {
        when(applicationInformationService.applicationInformation())
                .thenReturn(new ApplicationInformation(AIIDA_ID, Instant.EPOCH));
    }

    @Test
    void dispatch_processesAndPublishesAcknowledgement() throws Exception {
        var processingResult = InboundProcessingResult.accepted();
        var acknowledgementEnvelope = new AcknowledgementEnvelope();
        var inboundRecord = mock(InboundRecord.class);

        when(strategy.supportedSchema()).thenReturn(AiidaSchema.OPAQUE);
        when(strategy.process(inboundRecord)).thenReturn(processingResult);
        when(strategy.acknowledgementFormatter(AIIDA_ID)).thenReturn(formatter);
        when(formatter.convert(objectMapper, inboundRecord, processingResult)).thenReturn(acknowledgementEnvelope);
        when(objectMapper.writeValueAsBytes(acknowledgementEnvelope)).thenReturn(new byte[]{1, 2, 3});
        when(inboundRecord.schema()).thenReturn(AiidaSchema.OPAQUE);
        when(inboundRecord.dataSource()).thenReturn(dataSource);
        when(dataSource.id()).thenReturn(DATA_SOURCE_ID);
        when(dataSource.acknowledgementTopic()).thenReturn("ack/topic");
        when(mqttClientRegistry.clientFor(DATA_SOURCE_ID)).thenReturn(Optional.of(mqttClient));

        var dispatcher = dispatcher(List.of(strategy));
        dispatcher.dispatch(inboundRecord);

        verify(strategy).process(inboundRecord);
        verify(formatter).convert(objectMapper, inboundRecord, processingResult);
        verify(mqttClient).publish(anyString(), aryEq(new byte[]{1, 2, 3}), eq(0), eq(false));
    }

    @Test
    void constructor_rejectsMultipleStrategiesForOneSchema() {
        var duplicate = mock(InboundProcessingStrategy.class);
        when(strategy.supportedSchema()).thenReturn(AiidaSchema.OPAQUE);
        when(duplicate.supportedSchema()).thenReturn(AiidaSchema.OPAQUE);
        var strategies = List.of(strategy, duplicate);

        assertThrows(IllegalStateException.class, () -> dispatcher(strategies));
    }

    private InboundProcessingDispatcher dispatcher(List<InboundProcessingStrategy> strategies) {
        return new InboundProcessingDispatcher(
                inboundAggregator,
                mqttClientRegistry,
                objectMapper,
                applicationInformationService,
                strategies
        );
    }
}
