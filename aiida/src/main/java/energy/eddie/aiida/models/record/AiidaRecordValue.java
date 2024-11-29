package energy.eddie.aiida.models.record;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

@Entity
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
    @JsonProperty
    private String dataTag;
    @JsonProperty
    private String rawValue;
    @SuppressWarnings("unused") // Used when serialized to JSON
    @JsonProperty
    private String rawUnitOfMeasurement;
    @JsonProperty
    private String value;
    @SuppressWarnings("unused") // Used when serialized to JSON
    @JsonProperty
    private String unitOfMeasurement;

    @SuppressWarnings("NullAway.Init")
    public AiidaRecordValue(
            String rawTag, String dataTag,
            String rawValue, String rawUnitOfMeasurement, String value, String unitOfMeasurement
    ) {
        this.rawTag = rawTag;
        this.dataTag = dataTag;
        this.rawValue = rawValue;
        this.rawUnitOfMeasurement = rawUnitOfMeasurement;
        this.value = value;
        this.unitOfMeasurement = unitOfMeasurement;
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

    public String dataTag() {
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

    public void setValue(String value) {
        this.value = value;
    }
}
