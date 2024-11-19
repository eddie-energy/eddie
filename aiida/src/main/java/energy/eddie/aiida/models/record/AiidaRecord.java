package energy.eddie.aiida.models.record;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import energy.eddie.aiida.utils.InstantDeserializer;
import energy.eddie.aiida.utils.InstantSerializer;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.List;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class AiidaRecord {
    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // Used just as JPA ID
    @SuppressWarnings({"unused", "NullAway"})
    private Long id;
    @JsonProperty
    @JsonDeserialize(using = InstantDeserializer.class)
    @JsonSerialize(using = InstantSerializer.class)
    protected Instant timestamp;
    @JsonProperty
    protected String asset;
    @OneToMany(mappedBy = "aiidaRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonProperty("values")
    private List<AiidaRecordValue> aiidaRecordValues;

    @SuppressWarnings("NullAway.Init")
    protected AiidaRecord(Instant timestamp, String asset) {
        this.timestamp = timestamp;
        this.asset = asset;
    }

    public AiidaRecord(Instant timestamp, String asset, List<AiidaRecordValue> aiidaRecordValues) {
        this.timestamp = timestamp;
        this.asset = asset;
        this.aiidaRecordValues = aiidaRecordValues;
    }

    /**
     * Constructor only for JPA.
     */
    @SuppressWarnings("NullAway.Init")
    protected AiidaRecord() {
    }

    public Instant timestamp() {
        return timestamp;
    }

    public List<AiidaRecordValue> aiidaRecordValue() {
        return aiidaRecordValues;
    }

    public String asset() {
        return asset;
    }

    public Long id() {
        return id;
    }
}
