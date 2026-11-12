// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.inbound;

import energy.eddie.aiida.models.record.InboundProcessingResult;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.api.agnostic.aiida.AiidaSchema;

/**
 * Processes one inbound schema and reports whether the document was usable.
 */
public interface InboundProcessingStrategy {
    AiidaSchema supportedSchema();

    InboundProcessingResult process(InboundRecord inboundRecord);
}
