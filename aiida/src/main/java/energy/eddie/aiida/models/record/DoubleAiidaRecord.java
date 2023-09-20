package energy.eddie.aiida.models.record;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public class DoubleAiidaRecord extends AiidaRecord {
    @JsonProperty
    protected Double value;

    public DoubleAiidaRecord(Instant timestamp, String code, Double value) {
        super(timestamp, code);
        this.value = value;
    }

    public Double value() {
        return value;
    }
}