// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.requests;

import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;
import static java.util.Objects.requireNonNull;

/**
 * Creates EDA {@code GroupingId} values. The EDA schemas use this shared type for both
 * {@code MessageId} and {@code ConversationId}; newly initiated processes use the generated
 * value for both fields.
 */
@Component
public final class EdaGroupingIdFactory {
    private static final String PREFIX_PROPERTY = "region-connector.at.eda.conversation-id.prefix";

    private final AtConfiguration configuration;

    public EdaGroupingIdFactory(AtConfiguration configuration) {
        this.configuration = requireNonNull(configuration);

        var validationTime = ZonedDateTime.now(AT_ZONE_ID);
        validateConfiguredLength(AtConfiguration.PartyIdType.ELIGIBLE_PARTY, validationTime);
        if (configuration.energyCommunityId() != null) {
            validateConfiguredLength(AtConfiguration.PartyIdType.ENERGY_COMMUNITY, validationTime);
        }
    }

    public String create(AtConfiguration.PartyIdType type, ZonedDateTime dateTime) {
        var partyId = requireNonNull(configuration.partyIdFor(type));
        var address = configuration.conversationIdPrefix() + partyId;
        return new MessageId(address, dateTime).toString();
    }

    private void validateConfiguredLength(AtConfiguration.PartyIdType type, ZonedDateTime validationTime) {
        try {
            create(type, validationTime);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "%s is too long for the configured %s ID: %s"
                            .formatted(PREFIX_PROPERTY, partyTypeDescription(type), exception.getMessage())
            );
        }
    }

    private static String partyTypeDescription(AtConfiguration.PartyIdType type) {
        return switch (type) {
            case ELIGIBLE_PARTY -> "eligible party";
            case ENERGY_COMMUNITY -> "energy community party";
        };
    }
}
