package energy.eddie.aiida.models.record;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.time.Instant;


@Entity
@DiscriminatorValue("StringAiidaRecord")
public class StringAiidaRecord extends AiidaRecord {
    @JsonProperty
    @Column(name = "string_value")
    private String value;

    /**
     * Constructor only for JPA.
     */
    @SuppressWarnings("NullAway.Init")
    protected StringAiidaRecord() {
    }

    public StringAiidaRecord(Instant timestamp, String code, String value) {
        super(timestamp, code);
        this.value = value;
    }

    public String value() {
        return value;
    }
}