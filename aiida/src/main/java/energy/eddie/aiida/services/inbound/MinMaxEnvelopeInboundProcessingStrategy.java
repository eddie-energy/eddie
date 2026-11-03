// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.inbound;

import energy.eddie.aiida.adapters.datasource.inbound.ack.AckFormatterStrategy;
import energy.eddie.aiida.adapters.datasource.inbound.ack.cim.MinMaxEnvelopeAckFormatterStrategy;
import energy.eddie.aiida.models.record.InboundProcessingResult;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.aiida.services.connectionlimit.ConnectionLimitPersistenceService;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MinMaxEnvelopeInboundProcessingStrategy implements InboundProcessingStrategy {
    private final ConnectionLimitPersistenceService connectionLimitPersistenceService;

    public MinMaxEnvelopeInboundProcessingStrategy(
            ConnectionLimitPersistenceService connectionLimitPersistenceService
    ) {
        this.connectionLimitPersistenceService = connectionLimitPersistenceService;
    }

    @Override
    public AiidaSchema supportedSchema() {
        return AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12;
    }

    @Override
    public InboundProcessingResult process(InboundRecord inboundRecord) {
        return connectionLimitPersistenceService.process(inboundRecord);
    }

    @Override
    public AckFormatterStrategy acknowledgementFormatter(UUID aiidaId) {
        return new MinMaxEnvelopeAckFormatterStrategy(aiidaId);
    }
}
