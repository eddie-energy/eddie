// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.inbound;

import energy.eddie.aiida.models.record.InboundProcessingResult;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.aiida.services.connectionlimit.ConnectionLimitPersistenceService;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InboundProcessingStrategyTest {
    @Test
    void opaqueStrategy_acceptsRecordImmediately() {
        var strategy = new OpaqueInboundProcessingStrategy();

        assertEquals(AiidaSchema.OPAQUE, strategy.supportedSchema());
        assertEquals(InboundProcessingResult.accepted(), strategy.process(mock(InboundRecord.class)));
    }

    @Test
    void minMaxStrategy_delegatesToConnectionLimitPersistence() {
        var persistenceService = mock(ConnectionLimitPersistenceService.class);
        var inboundRecord = mock(InboundRecord.class);
        var processingResult = InboundProcessingResult.partiallyAccepted("one point ignored");
        when(persistenceService.process(inboundRecord)).thenReturn(processingResult);
        var strategy = new MinMaxEnvelopeInboundProcessingStrategy(persistenceService);

        assertEquals(AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12, strategy.supportedSchema());
        assertEquals(processingResult, strategy.process(inboundRecord));
    }
}
