// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.inbound;

import energy.eddie.aiida.aggregator.InboundAggregator;
import energy.eddie.aiida.models.record.InboundProcessingResult;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InboundProcessingDispatcherTest {
    @Mock
    private InboundAggregator inboundAggregator;
    @Mock
    private AcknowledgementPublisher acknowledgementPublisher;
    @Mock
    private InboundProcessingStrategy strategy;

    @Test
    void dispatch_processesAndPublishesAcknowledgement() {
        var processingResult = InboundProcessingResult.accepted();
        var inboundRecord = mock(InboundRecord.class);

        when(strategy.supportedSchema()).thenReturn(AiidaSchema.OPAQUE);
        when(strategy.process(inboundRecord)).thenReturn(processingResult);
        when(inboundRecord.schema()).thenReturn(AiidaSchema.OPAQUE);

        var dispatcher = dispatcher(List.of(strategy));
        dispatcher.dispatch(inboundRecord);

        verify(strategy).process(inboundRecord);
        verify(acknowledgementPublisher).publish(inboundRecord, processingResult);
    }

    @Test
    void dispatch_processingFailure_publishesRejection() {
        var inboundRecord = mock(InboundRecord.class);

        when(strategy.supportedSchema()).thenReturn(AiidaSchema.OPAQUE);
        when(strategy.process(inboundRecord)).thenThrow(new IllegalStateException("boom"));
        when(inboundRecord.schema()).thenReturn(AiidaSchema.OPAQUE);

        var dispatcher = dispatcher(List.of(strategy));
        dispatcher.dispatch(inboundRecord);

        var result = InboundProcessingResult.rejected("AIIDA could not process the inbound document: boom");
        verify(acknowledgementPublisher).publish(inboundRecord, result);
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
                acknowledgementPublisher,
                strategies
        );
    }
}
