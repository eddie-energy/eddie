// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.dataneeds.needs.aiida;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.api.agnostic.aiida.AiidaAsset;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.api.agnostic.aiida.ObisCodeConverter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;

import java.util.HashSet;
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

    @Schema(description = "The kinds of asset the data may be retrieved from ('CONNECTION-AGREEMENT-POINT', 'CONTROLLABLE-UNIT', 'DEDICATED-MEASUREMENT-DEVICE', 'SUBMETER'). An empty or missing list allows any asset.")
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "aiida_data_need_assets",
            joinColumns = @JoinColumn(name = "data_need_id"),
            schema = "data_needs")
    @Column(name = "asset")
    @JsonProperty
    @Enumerated(EnumType.STRING)
    // Mutable, since Hibernate adds the elements to the default value of a new instance when merging
    private Set<AiidaAsset> assets = new HashSet<>();

    @SuppressWarnings("NullAway.Init")
    public OutboundAiidaDataNeed() {
        // Default constructor for JPA
    }

    @Override
    public Set<AiidaSchema> supportedSchemas() {
        return SUPPORTED_SCHEMAS;
    }

    @Override
    public Set<AiidaAsset> assets() {
        return assets == null ? Set.of() : assets;
    }

    @Override
    public Set<ObisCode> dataTags() {
        return dataTags;
    }
}