package energy.eddie.aiida.models.record;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.utils.ObisCode;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.util.Objects;

@Entity
@RequireDataTagOrSourceKey
public class AiidaRecordValue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;

    @ManyToOne
    @JoinColumn(name = "aiida_record_id", referencedColumnName = "id")
    @JsonIgnore
    private AiidaRecord aiidaRecord;

    @JsonProperty
    private String rawTag;

    @Nullable
    @JsonProperty
    @Enumerated(EnumType.STRING)
    private ObisCode dataTag;

    @JsonProperty
    private String rawValue;

    @SuppressWarnings("unused") // Used when serialized to JSON
    @JsonProperty
    @Enumerated(EnumType.STRING)
    private UnitOfMeasurement rawUnitOfMeasurement;

    @JsonProperty
    private String value;

    @SuppressWarnings("unused") // Used when serialized to JSON
    @JsonProperty
    @Enumerated(EnumType.STRING)
    private UnitOfMeasurement unitOfMeasurement;

    @Nullable
    @JsonProperty
    @Column(name = "source_key")
    private String sourceKey;

    @SuppressWarnings("NullAway.Init")
    public AiidaRecordValue(
            String rawTag,
            @Nullable ObisCode dataTag,
            String rawValue,
            UnitOfMeasurement rawUnitOfMeasurement,
            String value,
            UnitOfMeasurement unitOfMeasurement
    ) {
        Objects.requireNonNull(dataTag, "Data tag cannot be null here");
        this.rawTag = rawTag;
        this.dataTag = dataTag;
        this.rawValue = rawValue;
        this.rawUnitOfMeasurement = rawUnitOfMeasurement;
        this.value = value;
        this.unitOfMeasurement = unitOfMeasurement;
        this.sourceKey = null;
    }

    @SuppressWarnings("NullAway.Init")
    public AiidaRecordValue(
            String rawTag,
            @Nullable String sourceKey,
            String rawValue,
            UnitOfMeasurement rawUnitOfMeasurement,
            String value,
            UnitOfMeasurement unitOfMeasurement
    ) {
        Objects.requireNonNull(sourceKey, "Source key cannot be null here");
        this.rawTag = rawTag;
        this.dataTag = null;
        this.rawValue = rawValue;
        this.rawUnitOfMeasurement = rawUnitOfMeasurement;
        this.value = value;
        this.unitOfMeasurement = unitOfMeasurement;
        this.sourceKey = sourceKey;
    }

    @SuppressWarnings("NullAway.Init")
    public AiidaRecordValue() {
    }

    public Long id() {
        return id;
    }

    public AiidaRecord aiidaRecord() {
        return aiidaRecord;
    }

    public void setAiidaRecord(AiidaRecord aiidaRecord) {
        this.aiidaRecord = aiidaRecord;
    }

    @Nullable
    public ObisCode dataTag() {
        return dataTag;
    }

    public String rawTag() {
        return rawTag;
    }

    public String value() {
        return value;
    }

    public String rawValue() {
        return rawValue;
    }

    public UnitOfMeasurement unitOfMeasurement() {
        return unitOfMeasurement;
    }

    public UnitOfMeasurement rawUnitOfMeasurement() {
        return rawUnitOfMeasurement;
    }

    @Nullable
    public String sourceKey() { return sourceKey; }

    /*
     * Returns either the dataTag or the sourceKey
     * 'return ""'
     * is fine here as we don't expect both null at the same time
     * ensured through @RequireDataTagOrSourceKey
     */
    public String dataPointKey() {
        if (dataTag != null) {
            return dataTag.toString();
        }
        if (sourceKey != null) {
            return sourceKey;
        }
        return "";
    }
}
