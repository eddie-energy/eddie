// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.dataneeds.needs;


import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.dataneeds.validation.BasicValidationsGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Entity
@Table(name = "energy_community_data_need", schema = "data_needs")
@Schema(description = "Data need for adding a new final customer to an existing energy community")
public class EnergyCommunityDataNeed extends DataNeed {
    public static final String DISCRIMINATOR_VALUE = "energy-community";
    @Column(name = "participation_factor", nullable = false)
    @JsonProperty(required = true)
    @NotNull(groups = BasicValidationsGroup.class, message = "must not be null")
    @Positive
    private final BigDecimal participationFactor;

    @SuppressWarnings("NullAway")
    protected EnergyCommunityDataNeed() {
        this.participationFactor = null;
    }

    public EnergyCommunityDataNeed(BigDecimal participationFactor) {
        super();
        this.participationFactor = participationFactor;
    }

    public BigDecimal participationFactor() {
        return participationFactor;
    }
}
