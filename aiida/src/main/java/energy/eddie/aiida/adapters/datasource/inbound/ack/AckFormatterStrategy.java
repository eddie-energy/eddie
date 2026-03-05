// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.inbound.ack;

import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.cim.v1_12.ack.AcknowledgementEnvelope;
import tools.jackson.databind.ObjectMapper;

public interface AckFormatterStrategy {
    AcknowledgementEnvelope convert(ObjectMapper objectMapper, InboundRecord inboundRecord);
}
