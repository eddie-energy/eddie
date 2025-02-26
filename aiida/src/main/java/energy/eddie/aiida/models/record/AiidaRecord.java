package energy.eddie.aiida.models.record;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "UTC")
    protected Instant timestamp;
    @JsonProperty
    protected String asset;
    @JsonProperty
    protected UUID dataSourceId;
    @OneToMany(mappedBy = "aiidaRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonProperty("values")
    private List<AiidaRecordValue> aiidaRecordValues;

    @SuppressWarnings("NullAway.Init")
    protected AiidaRecord(Instant timestamp, String asset, UUID dataSourceId) {
        this.timestamp = timestamp;
        this.asset = asset;
        this.dataSourceId = dataSourceId;
    }

    public AiidaRecord(Instant timestamp, String asset, UUID dataSourceId, List<AiidaRecordValue> aiidaRecordValues) {
        this.timestamp = timestamp;
        this.asset = asset;
        this.aiidaRecordValues = aiidaRecordValues;
        this.dataSourceId = dataSourceId;
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

    public UUID dataSourceId() {
        return dataSourceId;
    }
}
