package energy.eddie.core.dataneeds;

import energy.eddie.api.agnostic.DataNeed;
import energy.eddie.api.agnostic.DataType;
import energy.eddie.api.v0.ConsumptionRecord;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Record defining the attributes of a data need.
 * <p>
 * See <a href="https://eddie-web.projekte.fh-hagenberg.at/docs/requirements/4_data_requirements/1_logical_data_model/">DataNeedImpl in logical data model</a>
 */
@Entity
public class DataNeedImpl implements DataNeed {
    @Id
    private String id;
    private String description;
    private DataType type;
    private @Nullable ConsumptionRecord.MeteringInterval granularity;
    private Integer durationStart;
    private Boolean durationOpenEnd;
    private @Nullable Integer durationEnd;

    @SuppressWarnings("NullAway.Init")
    protected DataNeedImpl() {
    }

    public DataNeedImpl(String id, String description, DataType type, @Nullable ConsumptionRecord.MeteringInterval granularity, Integer durationStart, Boolean durationOpenEnd, @Nullable Integer durationEnd) {
        this.id = id;
        this.description = description;
        this.type = type;
        this.granularity = granularity;
        this.durationStart = durationStart;
        this.durationOpenEnd = durationOpenEnd;
        this.durationEnd = durationEnd;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DataType getType() {
        return type;
    }

    public void setType(DataType type) {
        this.type = type;
    }

    @Nullable
    public ConsumptionRecord.MeteringInterval getGranularity() {
        return granularity;
    }

    public void setGranularity(@Nullable ConsumptionRecord.MeteringInterval granularity) {
        this.granularity = granularity;
    }

    public Integer getDurationStart() {
        return durationStart;
    }

    public void setDurationStart(Integer durationStart) {
        this.durationStart = durationStart;
    }

    public Boolean getDurationOpenEnd() {
        return durationOpenEnd;
    }

    public void setDurationOpenEnd(Boolean durationOpenEnd) {
        this.durationOpenEnd = durationOpenEnd;
    }

    @Nullable
    public Integer getDurationEnd() {
        return durationEnd;
    }

    public void setDurationEnd(@Nullable Integer durationEnd) {
        this.durationEnd = durationEnd;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DataNeedImpl dataNeed)) return false;
        return Objects.equals(id, dataNeed.id) && Objects.equals(description, dataNeed.description) && type == dataNeed.type && granularity == dataNeed.granularity && Objects.equals(durationStart, dataNeed.durationStart) && Objects.equals(durationOpenEnd, dataNeed.durationOpenEnd) && Objects.equals(durationEnd, dataNeed.durationEnd);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, description, type, granularity, durationStart, durationOpenEnd, durationEnd);
    }
}
