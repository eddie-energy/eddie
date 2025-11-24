// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.permission.dataneed;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.api.agnostic.aiida.ObisCodeConverter;
import energy.eddie.api.agnostic.aiida.AiidaAsset;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import energy.eddie.dataneeds.needs.aiida.AiidaDataNeed;
import energy.eddie.dataneeds.needs.aiida.AiidaDataNeedInterface;
import energy.eddie.dataneeds.utils.cron.CronExpressionConverter;
import energy.eddie.dataneeds.utils.cron.CronExpressionDeserializer;
import energy.eddie.dataneeds.utils.cron.CronExpressionSerializer;
import jakarta.persistence.*;
import org.springframework.scheduling.support.CronExpression;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Stores the locally required information about a data need of a permission.
 */
@Entity
@DiscriminatorColumn(name = "type")
@Table(name = "aiida_local_data_need")
@SuppressWarnings("NullAway")
public abstract class AiidaLocalDataNeed implements AiidaDataNeedInterface {
    @Id
    @Column(nullable = false, name = "data_need_id")
    @JsonProperty
    protected UUID dataNeedId;

    @Column(name = "type", insertable = false, updatable = false)
    @JsonProperty
    protected String type;

    @Column(nullable = false)
    @JsonProperty
    protected String name;

    @Column(nullable = false)
    @JsonProperty
    protected String purpose;

    @Column(nullable = false, name = "policy_link")
    @JsonProperty
    protected String policyLink;

    @Column(nullable = false, name = "transmission_schedule")
    @Convert(converter = CronExpressionConverter.class)
    @JsonProperty
    @JsonSerialize(using = CronExpressionSerializer.class)
    @JsonDeserialize(using = CronExpressionDeserializer.class)
    protected CronExpression transmissionSchedule;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "aiida_local_data_need_schemas", joinColumns = {@JoinColumn(name = "data_need_id", referencedColumnName = "data_need_id")})
    @Column(name = "schema")
    @JsonProperty
    @Enumerated(EnumType.STRING)
    protected Set<AiidaSchema> schemas;

    @Column(nullable = false, name = "asset")
    @JsonProperty
    @Enumerated(EnumType.STRING)
    protected AiidaAsset asset;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "aiida_local_data_need_data_tags", joinColumns = {@JoinColumn(name = "data_need_id", referencedColumnName = "data_need_id")})
    @Column(name = "data_tag")
    @Convert(converter = ObisCodeConverter.class)
    @JsonProperty
    protected Set<ObisCode> dataTags;

    /**
     * Constructor only for JPA.
     */
    @SuppressWarnings("NullAway")
    protected AiidaLocalDataNeed() {
    }

    protected AiidaLocalDataNeed(AiidaDataNeed dataNeed) {
        this.dataNeedId = dataNeed.dataNeedId();
        this.type = dataNeed.type();
        this.name = dataNeed.name();
        this.purpose = dataNeed.purpose();
        this.policyLink = dataNeed.policyLink();
        this.transmissionSchedule = dataNeed.transmissionSchedule();
        this.schemas = dataNeed.schemas();
        this.asset = dataNeed.asset();
        this.dataTags = Objects.requireNonNullElse(dataNeed.dataTags(), Set.of());
    }

    public String name() {
        return name;
    }

    @Override
    public AiidaAsset asset() {
        return asset;
    }

    @Override
    public UUID dataNeedId() {
        return dataNeedId;
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

    @Override
    public String type() {
        return type;
    }
}
