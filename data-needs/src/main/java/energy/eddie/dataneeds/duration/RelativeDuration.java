package energy.eddie.dataneeds.duration;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.dataneeds.validation.duration.IsValidRelativeDuration;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.time.Period;
import java.util.Optional;

@Entity
@Table(name = "relative_duration", schema = "data_needs")
@IsValidRelativeDuration
@Schema(description = "Describes a duration with a relative start/end date. A date-period is specified which will be added/subtracted from the current date at the creation time of a permission request, i.e. when the customer clicks 'Connect' in the pop-up.")
public class RelativeDuration extends DataNeedDuration {
    public static final String DISCRIMINATOR_VALUE = "relativeDuration";

    @Schema(description = "ISO8601 duration string representing the period that should be added or subtracted from the current date to get the permission start date, when a new permission request for this data need is created. If not supplied, a region connector will use the earliest date possible date as permission request start date.", example = "-P3M", type = "string")
    @Column(name = "relative_start")
    @Nullable
    @JsonProperty
    private Period start;
    @Schema(description = "ISO8601 duration string representing the period that should be added or subtracted from the current date to get the permission end date, when a new permission request for this data need is created. If not supplied, a region connector will use the latest possible date as permission request end date.", example = "P1Y4M12D", type = "string")
    @Column(name = "relative_end")
    @Nullable
    @JsonProperty
    private Period end;
    @Schema(description = "The CalendarUnit to which the start of the relative duration should stick to. E.g., assume today is the 17.04., the relative start is -25 days and CalendarUnit is MONTH, then, for a permission request that is created today, the start date should not be the 23.03, but it should stick to the start of the CalendarUnit, and therefore be 01.03. Furthermore, for a permission request that is created on the 26.04, the start date should be the 01.04 and so forth. For the CalendarUnit WEEK, it will stick to Mondays. Note that this may lead to an earlier start date than what is passed via the start period.")
    @Enumerated(EnumType.STRING)
    @Column(name = "calendar_unit")
    @Nullable
    @JsonProperty
    private CalendarUnit stickyStartCalendarUnit;

    @SuppressWarnings("NullAway.Init")
    protected RelativeDuration() {
    }

    /**
     * Returns the period that should be used as offset for the start. If empty, assume the earliest date that the
     * region connector supports.
     */
    public Optional<Period> start() {
        return Optional.ofNullable(start);
    }

    /**
     * Returns the period that should be used as offset for the end. If empty, assume the latest date that the region
     * connector supports.
     */
    public Optional<Period> end() {
        return Optional.ofNullable(end);
    }

    /**
     * Indicates the CalendarUnit to which the start of the relative duration should stick to.
     * <br>
     * E.g., assume today is the 17.04., the relative start is -25 days and {@link #stickyStartCalendarUnit()} is
     * {@link CalendarUnit#MONTH}, then, for a permission request that is created today, the start date should not be
     * 23.03, but it should stick to the start of the CalendarUnit, and therefore be 01.03. Furthermore, for a
     * permission request that is created on the 26.04, the start date should be 01.04 and so forth.
     * <br>
     * For the {@link CalendarUnit#WEEK} it should stick to Mondays.
     *
     * @return CalendarUnit to stick to, if empty it should be ignored.
     */
    public Optional<CalendarUnit> stickyStartCalendarUnit() {
        return Optional.ofNullable(stickyStartCalendarUnit);
    }
}
