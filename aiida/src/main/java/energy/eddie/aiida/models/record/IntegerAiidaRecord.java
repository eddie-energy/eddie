package energy.eddie.aiida.models.record;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public class IntegerAiidaRecord extends AiidaRecord {
    @JsonProperty
    protected Integer value;

    public IntegerAiidaRecord(Instant timestamp, String code, Integer value) {
        super(timestamp, code);
        this.value = value;
    }

    public Integer value() {
        return value;
    }
}