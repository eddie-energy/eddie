package energy.eddie.dataneeds.needs.aiida;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import energy.eddie.dataneeds.needs.TimeframedDataNeed;
import energy.eddie.dataneeds.validation.schema.IsValidSchema;
import energy.eddie.dataneeds.utils.cron.CronExpressionConverter;
import energy.eddie.dataneeds.utils.cron.CronExpressionDeserializer;
import energy.eddie.dataneeds.utils.cron.CronExpressionSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import org.springframework.scheduling.support.CronExpression;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

@Entity
@Table(schema = "data_needs")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@IsValidSchema
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
    @NotEmpty(message = "must contain at least one schema")
    private Set<String> schemas;

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
     * Returns the schema for the outgoing data (Raw, CIM, Saref)
     */
    public Set<String> schemas() {
        return schemas;
    }
}
