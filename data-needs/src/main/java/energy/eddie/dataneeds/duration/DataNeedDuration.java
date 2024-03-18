package energy.eddie.dataneeds.duration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AbsoluteDuration.class, name = AbsoluteDuration.DISCRIMINATOR_VALUE),
        @JsonSubTypes.Type(value = RelativeDuration.class, name = RelativeDuration.DISCRIMINATOR_VALUE)
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class DataNeedDuration {
    @JsonIgnore
    private String dataNeedId;

    @SuppressWarnings("NullAway.Init")
    protected DataNeedDuration() {
    }

    /**
     * Returns the ID of the data need with which this {@link DataNeedDuration} is associated.
     */
    public String dataNeedId() {
        return dataNeedId;
    }

    /**
     * Sets the id of the data need with which this {@link DataNeedDuration} should be associated.
     */
    public void setDataNeedId(String dataNeedId) {
        this.dataNeedId = dataNeedId;
    }
}
