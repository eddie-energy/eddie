// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.permission.dataneed;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.api.agnostic.aiida.ObisCodeConverter;
import energy.eddie.dataneeds.needs.aiida.OutboundAiidaDataNeed;
import energy.eddie.dataneeds.needs.aiida.OutboundAiidaDataNeedInterface;
import jakarta.persistence.*;

import java.util.Objects;
import java.util.Set;

@Entity
@DiscriminatorValue(OutboundAiidaDataNeed.DISCRIMINATOR_VALUE)
@SuppressWarnings("NullAway")
public class OutboundAiidaLocalDataNeed extends AiidaLocalDataNeed implements OutboundAiidaDataNeedInterface {
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "aiida_local_data_need_data_tags", joinColumns = {@JoinColumn(name = "data_need_id", referencedColumnName = "data_need_id")})
    @Column(name = "data_tag")
    @Convert(converter = ObisCodeConverter.class)
    @JsonProperty
    protected Set<ObisCode> dataTags;

    @SuppressWarnings("NullAway.Init")
    protected OutboundAiidaLocalDataNeed() {
    }

    public OutboundAiidaLocalDataNeed(OutboundAiidaDataNeed dataNeed) {
        super(dataNeed);
        this.dataTags = Objects.requireNonNullElse(dataNeed.dataTags(), Set.of());
    }

    @Override
    public Set<ObisCode> dataTags() {
        return dataTags;
    }
}