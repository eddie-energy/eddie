package energy.eddie.dataneeds.needs;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.dataneeds.EnergyType;
import energy.eddie.dataneeds.validation.BasicValidationsGroup;
import energy.eddie.dataneeds.validation.CustomValidationsGroup;
import energy.eddie.dataneeds.validation.IsValidValidatedHistoricalDataDataNeed;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.NotNull;

/**
 * A data need designed to request validated historical data from the MDA. If the {@link #duration()} ends in the
 * future, new data should be fetched daily.
 */
@Entity
@Table(name = "validated_consumption_data_need", schema = "data_needs")
@GroupSequence({BasicValidationsGroup.class, CustomValidationsGroup.class, ValidatedHistoricalDataDataNeed.class})
@IsValidValidatedHistoricalDataDataNeed
@Schema(description = "Data need for validated historical consumption data. For most MDAs, data for the previous day is made available sometime during the current day (e.g. at noon).")
public class ValidatedHistoricalDataDataNeed extends TimeframedDataNeed {
    public static final String DISCRIMINATOR_VALUE = "validated";

    @Enumerated(EnumType.STRING)
    @Column(name = "energy_type", nullable = false)
    @JsonProperty(required = true)
    @NotNull(groups = BasicValidationsGroup.class, message = "must not be null")
    private EnergyType energyType;
    @Enumerated(EnumType.STRING)
    @Column(name = "min_granularity", nullable = false)
    @JsonProperty(required = true)
    @NotNull(groups = BasicValidationsGroup.class, message = "must not be null")
    private Granularity minGranularity;
    @Enumerated(EnumType.STRING)
    @Column(name = "max_granularity", nullable = false)
    @JsonProperty(required = true)
    @NotNull(groups = BasicValidationsGroup.class, message = "must not be null")
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
