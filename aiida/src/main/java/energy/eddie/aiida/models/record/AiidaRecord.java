package energy.eddie.aiida.models.record;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;

@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS,
        property = "type")
public abstract class AiidaRecord {
    @JsonProperty
    protected Instant timestamp;
    @JsonProperty
    protected String code;

    protected AiidaRecord(Instant timestamp, String code) {
        this.timestamp = timestamp;
        this.code = code;
    }

    public String code() {
        return code;
    }

    public Instant timestamp() {
        return timestamp;
    }
}
