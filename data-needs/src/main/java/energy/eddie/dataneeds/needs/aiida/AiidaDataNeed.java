// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.dataneeds.needs.aiida;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.api.agnostic.aiida.AiidaAsset;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.api.agnostic.aiida.ObisCodeConverter;
import energy.eddie.dataneeds.needs.TimeframedDataNeed;
import energy.eddie.dataneeds.utils.cron.CronExpressionConverter;
import energy.eddie.dataneeds.utils.cron.CronExpressionDeserializer;
import energy.eddie.dataneeds.utils.cron.CronExpressionSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.scheduling.support.CronExpression;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

import java.util.Set;
import java.util.UUID;

@Entity
@SuppressWarnings("NullAway")
public abstract class AiidaDataNeed extends TimeframedDataNeed implements AiidaDataNeedInterface {
    @Column(name = "transmission_schedule", nullable = false)
    @Convert(converter = CronExpressionConverter.class)
    @JsonProperty(required = true)
    @JsonSerialize(using = CronExpressionSerializer.class)
    @JsonDeserialize(using = CronExpressionDeserializer.class)
    @Schema(description = "The schedule in cron format, at which the AIIDA instance should send data.")
    private CronExpression transmissionSchedule;

    @Schema(description = "Define the schema for the outgoing data (Raw, CIM, Saref)")
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "aiida_data_need_schemas",
            joinColumns = @JoinColumn(name = "data_need_id"),
            schema = "data_needs")
    @Column(name = "schema")
    @JsonProperty(required = true)
    @Enumerated(EnumType.STRING)
    @NotEmpty(message = "must contain at least one schema")
    private Set<AiidaSchema> schemas;

    @Column(name = "asset", nullable = false)
    @JsonProperty(required = true)
    @Schema(description = "The kind of asset the data is retrieved from ('CONNECTION-AGREEMENT-POINT', 'CONTROLLABLE-UNIT', 'DEDICATED-MEASUREMENT-DEVICE', 'SUBMETER')")
    @Enumerated(EnumType.STRING)
    private AiidaAsset asset;

    @Column(name = "data_tag")
    @ElementCollection
    @CollectionTable(name = "aiida_data_need_data_tags",
            joinColumns = @JoinColumn(name = "data_need_id"),
            schema = "data_needs")
    @Convert(converter = ObisCodeConverter.class)
    @JsonProperty
    private Set<ObisCode> dataTags;

    @Override
    public AiidaAsset asset() {
        return asset;
    }

    @Override
    public UUID dataNeedId() {
        return UUID.fromString(id());
    }

    @Override
    public Set<ObisCode> dataTags() {
        return dataTags;
    }

    @Override
    public Set<AiidaSchema> schemas() {
        return schemas;
    }

    @Override
    public CronExpression transmissionSchedule() {
        return transmissionSchedule;
    }
}
