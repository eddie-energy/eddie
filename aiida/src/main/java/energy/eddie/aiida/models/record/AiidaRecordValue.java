package energy.eddie.aiida.models.record;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.utils.ObisCode;
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

    @SuppressWarnings("NullAway.Init")
    public AiidaRecordValue(
            String rawTag,
            ObisCode dataTag,
            String rawValue,
            UnitOfMeasurement rawUnitOfMeasurement,
            String value,
            UnitOfMeasurement unitOfMeasurement
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
}
