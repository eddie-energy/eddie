package energy.eddie.dataneeds.duration;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.dataneeds.validation.BasicValidationsGroup;
import energy.eddie.dataneeds.validation.CustomValidationsGroup;
import energy.eddie.dataneeds.validation.duration.IsValidAbsoluteDuration;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Entity
@Table(name = "absolute_duration", schema = "data_needs")
@IsValidAbsoluteDuration(groups = CustomValidationsGroup.class)
// when using custom sequence, the class itself has to be included (it is the "Default" group)
@GroupSequence({BasicValidationsGroup.class, CustomValidationsGroup.class, AbsoluteDuration.class})
@Schema(description = "Describes a duration with a fixed start/end date. When using an absolute duration, consider that most MDAs limit the timeframe for data requests (e.g. 2 years into the past), therefore at some point, it will not be possible anymore to fulfill the data need.")
public class AbsoluteDuration extends DataNeedDuration {
    public static final String DISCRIMINATOR_VALUE = "absoluteDuration";

    @Column(name = "absolute_start", nullable = false)
    @JsonProperty(required = true)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @NotNull(groups = BasicValidationsGroup.class, message = "must not be null")
    private LocalDate start;
    @Column(name = "absolute_end", nullable = false)
    @JsonProperty(required = true)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @NotNull(groups = BasicValidationsGroup.class, message = "must not be null")
    private LocalDate end;

    @SuppressWarnings("NullAway.Init")
    protected AbsoluteDuration() {
    }

    /**
     * Returns the inclusive start date of the timeframe for which data is required by the associated data need.
     */
    public LocalDate start() {
        return start;
    }

    /**
     * Returns the inclusive end date of the timeframe for which data is required by the associated data need.
     */
    public LocalDate end() {
        return end;
    }
}
