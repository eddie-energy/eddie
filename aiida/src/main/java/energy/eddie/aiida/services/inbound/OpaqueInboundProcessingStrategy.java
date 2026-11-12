// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.inbound;

import energy.eddie.aiida.models.record.InboundProcessingResult;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import org.springframework.stereotype.Component;

@Component
public class OpaqueInboundProcessingStrategy implements InboundProcessingStrategy {
    @Override
    public AiidaSchema supportedSchema() {
        return AiidaSchema.OPAQUE;
    }

    @Override
    public InboundProcessingResult process(InboundRecord inboundRecord) {
        return InboundProcessingResult.accepted();
    }
}
