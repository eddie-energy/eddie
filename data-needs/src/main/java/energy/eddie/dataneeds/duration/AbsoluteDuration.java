package energy.eddie.dataneeds.duration;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public class AbsoluteDuration extends DataNeedDuration {
    public static final String DISCRIMINATOR_VALUE = "absoluteDuration";

    @JsonProperty(required = true)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate start;
    @JsonProperty(required = true)
    @JsonFormat(pattern = "yyyy-MM-dd")
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
