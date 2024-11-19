package energy.eddie.dataneeds.needs.aiida;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import energy.eddie.dataneeds.needs.TimeframedDataNeed;
import energy.eddie.dataneeds.utils.cron.CronExpressionConverter;
import energy.eddie.dataneeds.utils.cron.CronExpressionDeserializer;
import energy.eddie.dataneeds.utils.cron.CronExpressionSerializer;
import energy.eddie.dataneeds.validation.asset.AiidaAsset;
import energy.eddie.dataneeds.validation.asset.IsValidAiidaAsset;
import energy.eddie.dataneeds.validation.schema.AiidaSchema;
import energy.eddie.dataneeds.validation.schema.IsValidAiidaSchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.scheduling.support.CronExpression;

import java.util.Set;

@Entity
@Table(schema = "data_needs")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@IsValidAiidaAsset
@IsValidAiidaSchema
public abstract class AiidaDataNeed extends TimeframedDataNeed {
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
    @Column(name = "schemas")
    @JsonProperty(required = true)
    @Enumerated(EnumType.STRING)
    @NotEmpty(message = "must contain at least one schema")
    private Set<AiidaSchema> schemas;

    @Column(name = "asset", nullable = false)
    @JsonProperty(required = true)
    @Schema(description = "The kind of asset the data is retrieved from ('CONNECTION-AGREEMENT-POINT', 'CONTROLLABLE-UNIT', 'DEDICATED-MEASUREMENT-DEVICE', 'SUBMETER')")
    @Enumerated(EnumType.STRING)
    private AiidaAsset asset;

    @SuppressWarnings("NullAway.Init")
    protected AiidaDataNeed() {
    }

    /**
     * Returns the schedule in cron format, at which the AIIDA instance should send data.
     */
    public CronExpression transmissionSchedule() {
        return transmissionSchedule;
    }

    /**
     * Returns the schema for the outgoing data
     *
     * @see AiidaSchema
     */
    public Set<AiidaSchema> schemas() {
        return schemas;
    }

    /**
     * Returns the kind of asset the data is retrieved from
     *
     * @see AiidaAsset
     */
    public AiidaAsset asset() {
        return asset;
    }
}
