package energy.eddie.dataneeds.duration;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "absolute_duration", schema = "data_needs")
public class AbsoluteDuration extends DataNeedDuration {
    public static final String DISCRIMINATOR_VALUE = "absoluteDuration";

    @Column(name = "absolute_start", nullable = false)
    @JsonProperty(required = true)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate start;
    @Column(name = "absolute_end", nullable = false)
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
