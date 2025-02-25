package energy.eddie.aiida.models.permission;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import energy.eddie.aiida.dtos.PermissionDetailsDto;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;
import energy.eddie.dataneeds.needs.aiida.AiidaDataNeedInterface;
import energy.eddie.dataneeds.needs.aiida.AiidaSchema;
import energy.eddie.dataneeds.utils.cron.CronExpressionConverter;
import energy.eddie.dataneeds.utils.cron.CronExpressionDefaults;
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
@SuppressWarnings("NullAway")
public class AiidaLocalDataNeed implements AiidaDataNeedInterface {
    @Id
    @Column(nullable = false, name = "data_need_id")
    @JsonProperty
    private final UUID dataNeedId;

    @Column(nullable = false)
    private final String type;

    @Column(nullable = false)
    @JsonProperty
    private final String name;

    @Column(nullable = false)
    @JsonProperty
    private final String purpose;

    @Column(nullable = false, name = "policy_link")
    @JsonProperty
    private final String policyLink;

    @Column(nullable = false, name = "transmission_schedule")
    @Convert(converter = CronExpressionConverter.class)
    @JsonProperty
    @JsonSerialize(using = CronExpressionSerializer.class)
    @JsonDeserialize(using = CronExpressionDeserializer.class)
    private final CronExpression transmissionSchedule;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "aiida_local_data_need_schemas", joinColumns = {@JoinColumn(name = "data_need_id", referencedColumnName = "data_need_id")})
    @Column(name = "schema")
    @JsonProperty
    @Enumerated(EnumType.STRING)
    private final Set<AiidaSchema> schemas;

    @Column(nullable = false, name = "asset")
    @JsonProperty
    @Enumerated(EnumType.STRING)
    private final AiidaAsset asset;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "aiida_local_data_need_data_tags", joinColumns = {@JoinColumn(name = "data_need_id", referencedColumnName = "data_need_id")})
    @Column(name = "data_tag")
    @JsonProperty
    private final Set<String> dataTags;

    /**
     * Constructor only for JPA.
     */
    @SuppressWarnings("NullAway")
    protected AiidaLocalDataNeed() {
        this.dataNeedId = UUID.randomUUID();
        this.type = "";
        this.name = "";
        this.purpose = "";
        this.policyLink = "";
        this.transmissionSchedule = CronExpression.parse(CronExpressionDefaults.MINUTELY.expression());
        this.schemas = Set.of();
        this.asset = AiidaAsset.CONNECTION_AGREEMENT_POINT;
        this.dataTags = Set.of();
    }

    public AiidaLocalDataNeed(PermissionDetailsDto details) {
        this.dataNeedId = details.dataNeed().dataNeedId();
        this.type = details.dataNeed().type();
        this.name = details.dataNeed().name();
        this.purpose = details.dataNeed().purpose();
        this.policyLink = details.dataNeed().policyLink();
        this.transmissionSchedule = details.dataNeed().transmissionSchedule();
        this.schemas = details.dataNeed().schemas();
        this.asset = details.dataNeed().asset();
        this.dataTags = requireNonNullElse(details.dataNeed().dataTags(), Set.of());
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
