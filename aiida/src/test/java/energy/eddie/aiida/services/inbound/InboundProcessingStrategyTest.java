// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.inbound;

import energy.eddie.aiida.models.record.InboundProcessingResult;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class InboundProcessingStrategyTest {
    @Test
    void opaqueStrategy_acceptsRecordImmediately() {
        var strategy = new OpaqueInboundProcessingStrategy();

        assertEquals(AiidaSchema.OPAQUE, strategy.supportedSchema());
        assertEquals(InboundProcessingResult.accepted(), strategy.process(mock(InboundRecord.class)));
    }
}
