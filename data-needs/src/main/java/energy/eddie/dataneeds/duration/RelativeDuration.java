package energy.eddie.dataneeds.duration;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

import java.time.Period;
import java.util.Optional;

public class RelativeDuration extends DataNeedDuration {
    public static final String DISCRIMINATOR_VALUE = "relativeDuration";

    @Nullable
    @JsonProperty
    private Period start;
    @Nullable
    @JsonProperty
    private Period end;
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
