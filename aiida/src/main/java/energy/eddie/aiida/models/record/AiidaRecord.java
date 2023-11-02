package energy.eddie.aiida.models.record;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.persistence.*;
import org.hibernate.annotations.DiscriminatorFormula;

import java.time.Instant;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = IntegerAiidaRecord.class, name = "IntegerAiidaRecord"),
        @JsonSubTypes.Type(value = StringAiidaRecord.class, name = "StringAiidaRecord")
})
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorFormula(
        "CASE WHEN integer_value IS NOT NULL THEN 'IntegerAiidaRecord' " +
                " WHEN string_value IS NOT NULL THEN 'StringAiidaRecord' end"
)
public abstract class AiidaRecord {
    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // Used just as JPA ID
    @SuppressWarnings({"Unused", "NullAway"})
    private Long id;
    @JsonProperty
    protected Instant timestamp;
    @JsonProperty
    protected String code;

    protected AiidaRecord(Instant timestamp, String code) {
        this.timestamp = timestamp;
        this.code = code;
    }

    /**
     * Constructor only for JPA.
     */
    @SuppressWarnings("NullAway.Init")
    protected AiidaRecord() {
    }

    public String code() {
        return code;
    }

    public Instant timestamp() {
        return timestamp;
    }
}
