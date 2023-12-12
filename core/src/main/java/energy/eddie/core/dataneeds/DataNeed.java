package energy.eddie.core.dataneeds;

import energy.eddie.api.v0.ConsumptionRecord;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Record defining the attributes of a data need.
 * <p>
 * See <a href="https://eddie-web.projekte.fh-hagenberg.at/docs/requirements/4_data_requirements/1_logical_data_model/">DataNeed in logical data model</a>
 */
@Entity
public class DataNeed {
    @Id
    @NotEmpty(message = "id is mandatory")
    private String id;

    @NotEmpty(message = "description is mandatory")
    private String description;

    @NotNull(message = "type is mandatory")
    private DataType type;

    private @Nullable ConsumptionRecord.MeteringInterval granularity;

    @NotNull(message = "durationStart is mandatory")
    private Integer durationStart;

    private Boolean durationOpenEnd;
    private @Nullable Integer durationEnd;

    @SuppressWarnings("NullAway.Init")
    protected DataNeed() {
    }

    public DataNeed(String id, String description, DataType type, @Nullable ConsumptionRecord.MeteringInterval granularity, Integer durationStart, Boolean durationOpenEnd, @Nullable Integer durationEnd) {
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
        if (!(o instanceof DataNeed)) return false;
        DataNeed dataNeed = (DataNeed) o;
        return Objects.equals(id, dataNeed.id) && Objects.equals(description, dataNeed.description) && type == dataNeed.type && granularity == dataNeed.granularity && Objects.equals(durationStart, dataNeed.durationStart) && Objects.equals(durationOpenEnd, dataNeed.durationOpenEnd) && Objects.equals(durationEnd, dataNeed.durationEnd);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, description, type, granularity, durationStart, durationOpenEnd, durationEnd);
    }

    private static final String TYPE_SPECIFIC_VIOLATION = "when type is %s: %s";

    /**
     * Validates the data and returns a collection of violations. It validates by using the Jakarta Bean Validation API
     * first. If there are no violations, it validates further a set of custom rules.
     *
     * @param validator The JSR380 validator instance to use.
     * @return A collection of strings representing the violations found during validation.
     * An empty collection indicates that the data is valid.
     */
    public List<String> validate(Validator validator) {
        var beanValidationViolations = validator.validate(this).stream().map(ConstraintViolation::getMessage).toList();
        if (!beanValidationViolations.isEmpty()) {
            return beanValidationViolations;
        }

        var violations = new ArrayList<String>();
        if (isOpenEnded() && durationEnd != null) {
            violations.add("durationOpenEnd and durationEnd are mutually exclusive");
        }
        if (!isOpenEnded() && durationEnd != null && durationStart > durationEnd) {
            violations.add("durationStart must be less than durationEnd");
        }
        if (type == DataType.FUTURE_VALIDATED_CONSUMPTION_DATA && durationStart < 0) {
            violations.add(TYPE_SPECIFIC_VIOLATION.formatted(type, "durationStart must be zero or positive"));
        }
        if (type != DataType.ACCOUNTING_POINT_MASTER_DATA && durationEnd == null && !isOpenEnded()) {
            violations.add(TYPE_SPECIFIC_VIOLATION.formatted(type, "durationEnd must be present or durationOpenEnd must be true"));
        }
        if (!(type == DataType.FUTURE_VALIDATED_CONSUMPTION_DATA || type == DataType.SMART_METER_P1_DATA) && durationEnd != null && durationEnd > 0) {
            violations.add(TYPE_SPECIFIC_VIOLATION.formatted(type, "durationEnd must be zero or negative"));
        }
        if (!(type == DataType.FUTURE_VALIDATED_CONSUMPTION_DATA || type == DataType.SMART_METER_P1_DATA) && isOpenEnded()) {
            violations.add(TYPE_SPECIFIC_VIOLATION.formatted(type, "durationOpenEnd must not be true"));
        }
        return violations;
    }

    private boolean isOpenEnded() {
        return durationOpenEnd != null && durationOpenEnd;
    }
}
