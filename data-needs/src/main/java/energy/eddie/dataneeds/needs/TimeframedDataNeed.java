package energy.eddie.dataneeds.needs;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.dataneeds.duration.DataNeedDuration;

public abstract class TimeframedDataNeed extends DataNeed {
    @JsonProperty(required = true)
    private DataNeedDuration duration;

    @SuppressWarnings("NullAway.Init")
    protected TimeframedDataNeed() {
    }

    public DataNeedDuration duration() {
        return duration;
    }
}
