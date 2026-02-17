// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic.data.needs;

import energy.eddie.api.agnostic.aiida.AiidaSchema;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


class AiidaDataNeedResultTest {
    private final Timeframe timeframe = new Timeframe(
            LocalDate.now(ZoneOffset.UTC),
            LocalDate.now(ZoneOffset.UTC).plusDays(1));

    @Test
    void supportsAllSchemas_whenAllRequiredSchemasPresent_returnsTrue() {
        var result = new AiidaDataNeedResult(
                Set.of(AiidaSchema.SMART_METER_P1_CIM_V1_12, AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12),
                Set.of(AiidaSchema.SMART_METER_P1_CIM_V1_12, AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12),
                timeframe
        );

        assertTrue(result.supportsAllSchemas());
    }

    @Test
    void supportsAllSchemas_whenAnyRequiredSchemaPresent_returnsTrue() {
        var result = new AiidaDataNeedResult(
                Set.of(AiidaSchema.SMART_METER_P1_CIM_V1_12),
                Set.of(AiidaSchema.SMART_METER_P1_CIM_V1_12, AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12),
                timeframe
        );

        assertTrue(result.supportsAllSchemas());
    }

    @Test
    void supportsAllSchemas_whenNoRequiredSchemasPresent_returnsFalse() {
        var result = new AiidaDataNeedResult(
                Set.of(AiidaSchema.SMART_METER_P1_RAW, AiidaSchema.SMART_METER_P1_CIM_V1_12),
                Set.of(AiidaSchema.SMART_METER_P1_CIM_V1_12, AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12),
                timeframe
        );

        assertFalse(result.supportsAllSchemas());
    }
}
