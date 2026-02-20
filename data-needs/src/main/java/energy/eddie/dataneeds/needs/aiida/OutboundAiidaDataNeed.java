// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.dataneeds.needs.aiida;

import energy.eddie.api.agnostic.aiida.AiidaSchema;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.Set;

@Entity
@Table(name = "outbound_aiida_data_need", schema = "data_needs")
@DiscriminatorValue(OutboundAiidaDataNeed.DISCRIMINATOR_VALUE)
@SuppressWarnings("NullAway")
public class OutboundAiidaDataNeed extends AiidaDataNeed {
    public static final String DISCRIMINATOR_VALUE = "outbound-aiida";
    public static final Set<AiidaSchema> SUPPORTED_SCHEMAS = Set.of(AiidaSchema.SMART_METER_P1_RAW,
                                                                      AiidaSchema.SMART_METER_P1_CIM_V1_04,
                                                                      AiidaSchema.SMART_METER_P1_CIM_V1_12);

    @SuppressWarnings("NullAway.Init")
    public OutboundAiidaDataNeed() {
        // Default constructor for JPA
    }

    @Override
    public Set<AiidaSchema> supportedSchemas() {
        return SUPPORTED_SCHEMAS;
    }
}