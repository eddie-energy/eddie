package energy.eddie.dataneeds.needs;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.dataneeds.EnergyType;

/**
 * A data need designed to request validated historical data from the MDA. If the {@link #duration()} ends in the
 * future, new data should be fetched daily.
 */
public class ValidatedHistoricalDataDataNeed extends TimeframedDataNeed {
    public static final String DISCRIMINATOR_VALUE = "validated";

    @JsonProperty(required = true)
    private EnergyType energyType;
    @JsonProperty(required = true)
    private Granularity minGranularity;
    @JsonProperty(required = true)
    private Granularity maxGranularity;

    @SuppressWarnings("NullAway.Init")
    protected ValidatedHistoricalDataDataNeed() {
    }

    /**
     * Returns which type of energy should be requested for the associated data need.
     */
    public EnergyType energyType() {
        return energyType;
    }

    /**
     * Returns the granularity in which data should be requested from the MDA. If the data is not available in this
     * granularity and {@link #maxGranularity()} is a higher granularity, the region connector should automatically
     * retry to fetch the data in the next highest granularity that is supported by the region connector until data is
     * received. If no data can be retrieved for the granularity specified by {@link #maxGranularity()}, then the
     * permission request should be transitioned into an error state.
     */
    public Granularity minGranularity() {
        return minGranularity;
    }

    /**
     * Returns the highest acceptable data granularity.
     *
     * @see #minGranularity()
     */
    public Granularity maxGranularity() {
        return maxGranularity;
    }
}
