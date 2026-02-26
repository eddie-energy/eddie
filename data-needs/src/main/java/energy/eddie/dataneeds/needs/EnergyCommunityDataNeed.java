// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.dataneeds.needs;


import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.dataneeds.validation.BasicValidationsGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Range;

@Entity
@Table(name = "energy_community_data_need", schema = "data_needs")
@Schema(description = "Data need for adding a new final customer to an existing energy community")
public class EnergyCommunityDataNeed extends DataNeed {
    public static final String DISCRIMINATOR_VALUE = "energy-community";
    @Column(name = "participation_factor", nullable = false)
    @JsonProperty(required = true)
    @NotNull(groups = BasicValidationsGroup.class)
    @Range(min = 1, max = 100)
    private final Integer participationFactor;
    @Enumerated(EnumType.STRING)
    @Column(name = "min_granularity", nullable = false)
    @JsonProperty(required = true)
    @NotNull(groups = BasicValidationsGroup.class)
    private final Granularity minGranularity;
    @Enumerated(EnumType.STRING)
    @Column(name = "max_granularity", nullable = false)
    @JsonProperty(required = true)
    @NotNull(groups = BasicValidationsGroup.class)
    private final Granularity maxGranularity;
    @Enumerated(EnumType.STRING)
    @Column(name = "energy_direction", nullable = false)
    @JsonProperty(required = true)
    @NotNull(groups = BasicValidationsGroup.class)
    private final EnergyDirection energyDirection;

    @SuppressWarnings("NullAway")
    protected EnergyCommunityDataNeed() {
        this.minGranularity = null;
        this.maxGranularity = null;
        this.participationFactor = null;
        this.energyDirection = null;
    }

    public EnergyCommunityDataNeed(
            Integer participationFactor,
            Granularity minGranularity,
            Granularity maxGranularity,
            EnergyDirection energyDirection
    ) {
        super();
        this.participationFactor = participationFactor;
        this.minGranularity = minGranularity;
        this.maxGranularity = maxGranularity;
        this.energyDirection = energyDirection;
    }

    /**
     * The amount with which the final customer will participate in the energy community.
     *
     * @return the participation factor of the final customer in the energy community.
     */
    public Integer participationFactor() {
        return participationFactor;
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

    /**
     * Indicates whether the final customer should produce or consume energy.
     *
     * @return consumption, if the final customer is going to consume electricity produced by other members of the energy community, or if the final customer is going to produce energy for the other members.
     */
    public EnergyDirection energyDirection() {
        return energyDirection;
    }
}
