// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.config;

import jakarta.validation.constraints.AssertTrue;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Name;

import static java.util.Objects.requireNonNull;

/**
 * The main configuration for the EDA region connector.
 *
 * @param eligiblePartyId ID that will be used as the sender for all messages sent to EDA.
 *                        This ID must be registered with EDA at <a href="https://www.ebutilities.at/registrierung">ebUtilities</a>.
 */
@ConfigurationProperties("region-connector.at.eda")
public record AtConfiguration(@Name("eligibleparty.id") String eligiblePartyId, @Nullable String energyCommunityId,
                              @Nullable String energyCommunityPartyId) {
    @AssertTrue(message = "If the energy community ID has been set, the energy community party ID must also be set")
    public boolean isEnergyCommunityIdSetMustBeEnergyCommunityPartyIdAlsoSet() {
        return energyCommunityId != null && energyCommunityPartyId != null;
    }

    /**
     * Returns the party ID for the specific type.
     *
     * @param type whether the party ID of the energy community or the eligible party ID is needed.
     * @return the party ID for the specified type
     * @throws NullPointerException if the EnergyCommunity type is specified, but the energy community type is not configured, which should never happen.
     *                              If this exception is thrown, the API is used incorrectly or a bug has been introduced.
     */
    public String partyIdFor(PartyIdType type) {
        return switch (type) {
            case ENERGY_COMMUNITY -> requireNonNull(energyCommunityPartyId(),
                                                    "energyCommunityPartyId must not be null when requesting the party ID of an energy community");
            case ELIGIBLE_PARTY -> eligiblePartyId();
        };
    }

    public enum PartyIdType {
        ENERGY_COMMUNITY,
        ELIGIBLE_PARTY
    }
}
