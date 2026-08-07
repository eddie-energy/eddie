// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.dataneeds.needs.aiida;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.api.agnostic.aiida.ObisCodeConverter;
import jakarta.persistence.*;

import java.util.Set;

@Entity
@Table(name = "outbound_aiida_data_need", schema = "data_needs")
@DiscriminatorValue(OutboundAiidaDataNeed.DISCRIMINATOR_VALUE)
@SuppressWarnings("NullAway")
public class OutboundAiidaDataNeed extends AiidaDataNeed implements OutboundAiidaDataNeedInterface {
    public static final String DISCRIMINATOR_VALUE = "outbound-aiida";
    public static final Set<AiidaSchema> SUPPORTED_SCHEMAS = Set.of(AiidaSchema.SMART_METER_P1_RAW,
                                                                    AiidaSchema.SMART_METER_P1_CIM_V1_04,
                                                                    AiidaSchema.SMART_METER_P1_CIM_V1_12,
                                                                    AiidaSchema.OPAQUE,
                                                                    AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12);

    @Column(name = "data_tag")
    @ElementCollection
    @CollectionTable(name = "aiida_data_need_data_tags",
            joinColumns = @JoinColumn(name = "data_need_id"),
            schema = "data_needs")
    @Convert(converter = ObisCodeConverter.class)
    @JsonProperty
    private Set<ObisCode> dataTags;

    @SuppressWarnings("NullAway.Init")
    public OutboundAiidaDataNeed() {
        // Default constructor for JPA
    }

    @Override
    public Set<AiidaSchema> supportedSchemas() {
        return SUPPORTED_SCHEMAS;
    }

    @Override
    public Set<ObisCode> dataTags() {
        return dataTags;
    }
}