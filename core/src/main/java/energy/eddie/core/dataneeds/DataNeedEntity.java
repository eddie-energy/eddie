package energy.eddie.core.dataneeds;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.api.agnostic.DataNeed;
import energy.eddie.api.agnostic.DataType;
import energy.eddie.api.agnostic.Granularity;
import jakarta.annotation.Nullable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

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
    @JsonProperty
    private @Nullable Integer transmissionInterval;
    @ElementCollection(fetch = FetchType.EAGER)
    @JsonProperty
    private @Nullable Set<String> sharedDataIds;
    @JsonProperty
    private @Nullable String serviceName;

    @SuppressWarnings("NullAway.Init")
    protected DataNeedEntity() {
    }

    /**
     * Create a DataNeedEntity for any DataType except {@link DataType#AIIDA_NEAR_REALTIME_DATA}.
     * <br>
     * Use {@link #DataNeedEntity(String, String, DataType, Granularity, Integer, Boolean, Integer, Integer, Set, String)} for {@code AIIDA_NEAR_REALTIME_DATA}.
     */
    public DataNeedEntity(String id, String description, DataType type, Granularity granularity, Integer durationStart, Boolean durationOpenEnd, @Nullable Integer durationEnd) {
        this(id, description, type, granularity, durationStart, durationOpenEnd, durationEnd, null, Collections.emptySet(), null);
    }

    /**
     * The expected type for a DataNeedEntity created by this constructor is {@link DataType#AIIDA_NEAR_REALTIME_DATA},
     * therefore {@code transmissionInterval} and {@code sharedDataIds} have to be specified.
     */
    // These are all required fields for a DataNeed, therefore they all need to be specified in the constructor, resulting in many parameters
    @SuppressWarnings("java:S107")
    public DataNeedEntity(String id, String description, DataType type, Granularity granularity, Integer durationStart, Boolean durationOpenEnd, @Nullable Integer durationEnd, @Nullable Integer transmissionInterval, @Nullable Set<String> sharedDataIds, @Nullable String serviceName) {
        this.id = id;
        this.description = description;
        this.type = type;
        this.granularity = granularity;
        this.durationStart = durationStart;
        this.durationOpenEnd = durationOpenEnd;
        this.durationEnd = durationEnd;

        this.transmissionInterval = transmissionInterval;
        this.sharedDataIds = sharedDataIds;
        this.serviceName = serviceName;
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

    @Nullable
    @Override
    public Integer transmissionInterval() {
        return transmissionInterval;
    }

    public void setTransmissionInterval(@Nullable Integer transmissionInterval) {
        this.transmissionInterval = transmissionInterval;
    }

    @Nullable
    @Override
    public Set<String> sharedDataIds() {
        return sharedDataIds;
    }

    public void setSharedDataIds(@Nullable Set<String> sharedDataIds) {
        this.sharedDataIds = sharedDataIds;
    }

    @Nullable
    @Override
    public String serviceName() {
        return serviceName;
    }

    public void setServiceName(@Nullable String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DataNeedEntity dataNeed)) return false;
        return Objects.equals(id, dataNeed.id) && Objects.equals(description, dataNeed.description) && type == dataNeed.type && granularity == dataNeed.granularity && Objects.equals(durationStart, dataNeed.durationStart) && Objects.equals(durationOpenEnd, dataNeed.durationOpenEnd) && Objects.equals(durationEnd, dataNeed.durationEnd) && Objects.equals(transmissionInterval, dataNeed.transmissionInterval) && Objects.equals(sharedDataIds, dataNeed.sharedDataIds) && Objects.equals(serviceName, dataNeed.serviceName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, description, type, granularity, durationStart, durationOpenEnd, durationEnd, transmissionInterval, sharedDataIds, serviceName);
    }
}