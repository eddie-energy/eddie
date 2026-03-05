// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.inbound.ack;

import energy.eddie.aiida.adapters.datasource.inbound.ack.cim.MinMaxEnvelopeAckFormatterStrategy;
import energy.eddie.aiida.adapters.datasource.inbound.ack.raw.RawAckFormatterStrategy;
import energy.eddie.aiida.errors.formatter.CimSchemaFormatterException;
import energy.eddie.api.agnostic.aiida.AiidaSchema;

import java.util.UUID;

public class AckFormatterStrategyRegistry {
    public AckFormatterStrategy strategyFor(AiidaSchema schema, UUID aiidaId) throws CimSchemaFormatterException {
        return switch (schema) {
            case RAW -> new RawAckFormatterStrategy(aiidaId);
            case MIN_MAX_ENVELOPE_CIM_V1_12 -> new MinMaxEnvelopeAckFormatterStrategy(aiidaId);
            default -> throw new CimSchemaFormatterException(
                    new IllegalArgumentException("No CIM formatter strategy found for schema " + schema));
        };
    }
}
