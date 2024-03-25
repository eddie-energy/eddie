package energy.eddie.aiida.models.record;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.time.Instant;

@Entity
@DiscriminatorValue("IntegerAiidaRecord")
public class IntegerAiidaRecord extends AiidaRecord {
    @JsonProperty
    @Column(name = "integer_value")
    private Integer value;

    /**
     * Constructor only for JPA.
     */
    @SuppressWarnings("NullAway.Init")
    protected IntegerAiidaRecord() {
    }

    public IntegerAiidaRecord(Instant timestamp, String code, Integer value) {
        super(timestamp, code);
        this.value = value;
    }

    public Integer value() {
        return value;
    }
}