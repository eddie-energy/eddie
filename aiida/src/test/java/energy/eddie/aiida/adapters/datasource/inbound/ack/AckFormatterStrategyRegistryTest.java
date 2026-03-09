// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.inbound.ack;

import energy.eddie.aiida.adapters.datasource.inbound.ack.cim.MinMaxEnvelopeAckFormatterStrategy;
import energy.eddie.aiida.adapters.datasource.inbound.ack.opaque.OpaqueAckFormatterStrategy;
import energy.eddie.aiida.errors.formatter.CimSchemaFormatterException;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AckFormatterStrategyRegistryTest {
    private final AckFormatterStrategyRegistry registry = new AckFormatterStrategyRegistry();

    @Test
    void strategyFor_returnsRawAckFormatterStrategy_forRawCimV1_12() throws Exception {
        // When
        var strategy = registry.strategyFor(AiidaSchema.OPAQUE, UUID.randomUUID());

        // Then
        assertInstanceOf(OpaqueAckFormatterStrategy.class, strategy);
    }

    @Test
    void strategyFor_returnsMinMaxEnvelopeCimFormatterStrategy_forMinMaxEnvelopeCimV1_12() throws Exception {
        // When
        var strategy = registry.strategyFor(AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12, UUID.randomUUID());

        // Then
        assertInstanceOf(MinMaxEnvelopeAckFormatterStrategy.class, strategy);
    }

    @Test
    void strategyFor_throwsCimSchemaFormatterException_forUnsupportedSchema() {
        // When, Then
        assertThrows(CimSchemaFormatterException.class,
                     () -> registry.strategyFor(AiidaSchema.SMART_METER_P1_CIM_V1_04, UUID.randomUUID()));
    }
}
