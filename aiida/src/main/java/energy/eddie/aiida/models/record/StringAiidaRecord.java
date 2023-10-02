package energy.eddie.aiida.models.record;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public class StringAiidaRecord extends AiidaRecord {
    @JsonProperty
    protected String value;

    public StringAiidaRecord(Instant timestamp, String code, String value) {
        super(timestamp, code);
        this.value = value;
    }

    public String value() {
        return value;
    }
}