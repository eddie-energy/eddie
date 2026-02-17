// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.dataneeds.needs.aiida;

import energy.eddie.api.agnostic.aiida.AiidaSchema;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AiidaDataNeedTest {
    @Test
    void supportsAllSchemas_returnsTrue_forOutboundDataNeed() throws Exception {
        var dataNeed = dataNeedWithSchemas(
                new OutboundAiidaDataNeed(),
                Set.of(AiidaSchema.SMART_METER_P1_CIM_V1_04,
                       AiidaSchema.SMART_METER_P1_CIM_V1_12,
                       AiidaSchema.SMART_METER_P1_RAW)
        );

        assertThat(dataNeed.supportsAllSchemas()).isTrue();
    }

    @Test
    void supportsAllSchemas_returnsTrue_forInboundDataNeed() throws Exception {
        var dataNeed = dataNeedWithSchemas(
                new InboundAiidaDataNeed(),
                Set.of(AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12)
        );

        assertThat(dataNeed.supportsAllSchemas()).isTrue();
    }

    @Test
    void supportsAllSchemas_returnsFalse_forOutboundDataNeed() throws Exception {
        var dataNeed = dataNeedWithSchemas(
                new OutboundAiidaDataNeed(),
                Set.of(AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12)
        );

        assertThat(dataNeed.supportsAllSchemas()).isFalse();
    }

    @Test
    void supportsAllSchemas_returnsFalse_forInboundDataNeed() throws Exception {
        var dataNeed = dataNeedWithSchemas(
                new InboundAiidaDataNeed(),
                Set.of(AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12, AiidaSchema.SMART_METER_P1_CIM_V1_12)
        );

        assertThat(dataNeed.supportsAllSchemas()).isFalse();
    }

    private static AiidaDataNeed dataNeedWithSchemas(
            AiidaDataNeed dataNeed,
            Set<AiidaSchema> schemas
    ) throws Exception {
        var field = AiidaDataNeed.class.getDeclaredField("schemas");
        field.setAccessible(true);
        field.set(dataNeed, schemas);
        return dataNeed;
    }
}
