package energy.eddie.aiida.models.permission.dataneed;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import energy.eddie.dataneeds.needs.aiida.*;
import energy.eddie.dataneeds.utils.cron.CronExpressionConverter;
import energy.eddie.dataneeds.utils.cron.CronExpressionDeserializer;
import energy.eddie.dataneeds.utils.cron.CronExpressionSerializer;
import jakarta.persistence.*;
import org.springframework.scheduling.support.CronExpression;

import java.util.Set;
import java.util.UUID;

import static java.util.Objects.requireNonNullElse;

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
    @JsonProperty
    protected Set<String> dataTags;

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
        this.dataTags = requireNonNullElse(dataNeed.dataTags(), Set.of());
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
    public Set<String> dataTags() {
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
