package energy.eddie.core.dataneeds;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.api.agnostic.DataNeed;
import energy.eddie.api.agnostic.DataType;
import energy.eddie.api.agnostic.Granularity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Record defining the attributes of a data need.
 * <p>
 * See <a href="https://eddie-web.projekte.fh-hagenberg.at/docs/requirements/4_data_requirements/1_logical_data_model/">DataNeedEntity in logical data model</a>
 */
@Entity
public class DataNeedEntity implements DataNeed {
    @Id
    @JsonProperty
    private String id;
    @JsonProperty
    private String description;
    @JsonProperty
    private DataType type;
    @JsonProperty
    private Granularity granularity;
    @JsonProperty
    private Integer durationStart;
    @JsonProperty
    private Boolean durationOpenEnd;
    @JsonProperty
    private @Nullable Integer durationEnd;

    @SuppressWarnings("NullAway.Init")
    protected DataNeedEntity() {
    }

    public DataNeedEntity(String id, String description, DataType type, Granularity granularity, Integer durationStart, Boolean durationOpenEnd, @Nullable Integer durationEnd) {
        this.id = id;
        this.description = description;
        this.type = type;
        this.granularity = granularity;
        this.durationStart = durationStart;
        this.durationOpenEnd = durationOpenEnd;
        this.durationEnd = durationEnd;
    }

    @Override
    public String id() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String description() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public DataType type() {
        return type;
    }

    public void setType(DataType type) {
        this.type = type;
    }

    @Override
    public Granularity granularity() {
        return granularity;
    }

    public void setGranularity(Granularity granularity) {
        this.granularity = granularity;
    }

    @Override
    public Integer durationStart() {
        return durationStart;
    }

    public void setDurationStart(Integer durationStart) {
        this.durationStart = durationStart;
    }

    @Override
    public Boolean durationOpenEnd() {
        return durationOpenEnd;
    }

    public void setDurationOpenEnd(Boolean durationOpenEnd) {
        this.durationOpenEnd = durationOpenEnd;
    }

    @Override
    @Nullable
    public Integer durationEnd() {
        return durationEnd;
    }

    public void setDurationEnd(@Nullable Integer durationEnd) {
        this.durationEnd = durationEnd;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DataNeedEntity dataNeed)) return false;
        return Objects.equals(id, dataNeed.id) && Objects.equals(description, dataNeed.description) && type == dataNeed.type && granularity == dataNeed.granularity && Objects.equals(durationStart, dataNeed.durationStart) && Objects.equals(durationOpenEnd, dataNeed.durationOpenEnd) && Objects.equals(durationEnd, dataNeed.durationEnd);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, description, type, granularity, durationStart, durationOpenEnd, durationEnd);
    }
}