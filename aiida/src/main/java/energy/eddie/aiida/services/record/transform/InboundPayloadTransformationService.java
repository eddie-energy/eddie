// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.record.transform;

import energy.eddie.aiida.errors.record.UnsupportedInboundRecordTransformationException;
import energy.eddie.aiida.models.permission.InboundMessageFormat;
import energy.eddie.aiida.models.record.InboundRecord;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Service
public class InboundPayloadTransformationService {
    private final ObjectMapper objectMapper;
    private final List<InboundPayloadTransformer> transformers;

    public InboundPayloadTransformationService(
            ObjectMapper objectMapper,
            List<InboundPayloadTransformer> transformers
    ) {
        this.objectMapper = objectMapper;
        this.transformers = transformers;
    }

    public String transform(
            InboundRecord inboundRecord,
            InboundMessageFormat inboundMessageFormat
    ) throws UnsupportedInboundRecordTransformationException {
        for (var transformer : transformers) {
            if (transformer.supports(inboundRecord.schema(), inboundMessageFormat)) {
                return transformer.transform(objectMapper, inboundRecord);
            }
        }
        throw new UnsupportedInboundRecordTransformationException(inboundRecord.schema(), inboundMessageFormat);
    }
}
