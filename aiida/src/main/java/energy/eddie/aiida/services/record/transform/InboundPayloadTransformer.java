// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.record.transform;

import energy.eddie.aiida.models.permission.InboundMessageFormat;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import tools.jackson.databind.ObjectMapper;

public interface InboundPayloadTransformer {
    boolean supports(AiidaSchema schema, InboundMessageFormat inboundMessageFormat);

    String transform(ObjectMapper objectMapper, InboundRecord inboundRecord);
}
