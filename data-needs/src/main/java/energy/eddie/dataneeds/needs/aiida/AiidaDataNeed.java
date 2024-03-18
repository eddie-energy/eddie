package energy.eddie.dataneeds.needs.aiida;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.dataneeds.needs.TimeframedDataNeed;

public abstract class AiidaDataNeed extends TimeframedDataNeed {
    @JsonProperty(required = true)
    private int transmissionInterval;

    @SuppressWarnings("NullAway.Init")
    protected AiidaDataNeed() {
    }

    /**
     * Returns the interval in seconds, at which the AIIDA instance should send data.
     */
    public int transmissionInterval() {
        return transmissionInterval;
    }
}
