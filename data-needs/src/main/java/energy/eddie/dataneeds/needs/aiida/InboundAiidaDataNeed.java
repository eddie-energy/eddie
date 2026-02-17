// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.dataneeds.needs.aiida;

import energy.eddie.api.agnostic.aiida.AiidaSchema;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.List;

@Entity
@Table(name = "inbound_aiida_data_need", schema = "data_needs")
@DiscriminatorValue(InboundAiidaDataNeed.DISCRIMINATOR_VALUE)
@SuppressWarnings("NullAway")
public class InboundAiidaDataNeed extends AiidaDataNeed {
    public static final String DISCRIMINATOR_VALUE = "inbound-aiida";
    public static final List<AiidaSchema> SUPPORTED_SCHEMAS = List.of(AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12);

    @SuppressWarnings("NullAway.Init")
    public InboundAiidaDataNeed() {
        // Default constructor for JPA
    }

    @Override
    public List<AiidaSchema> supportedSchemas() {
        return SUPPORTED_SCHEMAS;
    }
}