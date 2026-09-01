// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.requests;

import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EdaGroupingIdFactoryTest {
    private static final ZonedDateTime DATE_TIME = ZonedDateTime.parse("2026-09-01T12:00:00Z");

    @Test
    void create_withPrefix_prependsPrefix() {
        var factory = new EdaGroupingIdFactory(new AtConfiguration("EP123456", null, null, "DEV1"));

        var result = factory.create(AtConfiguration.PartyIdType.ELIGIBLE_PARTY, DATE_TIME);

        assertEquals("DEV1EP123456T1788264000000", result);
    }

    @Test
    void create_withoutPrefix_usesPartyId() {
        var factory = new EdaGroupingIdFactory(new AtConfiguration("EP123456", null, null, ""));

        var result = factory.create(AtConfiguration.PartyIdType.ELIGIBLE_PARTY, DATE_TIME);

        assertEquals("EP123456T1788264000000", result);
    }

    @Test
    void create_forEnergyCommunity_usesEnergyCommunityPartyId() {
        var factory = new EdaGroupingIdFactory(
                new AtConfiguration("EP123456", "community", "EC123456", "DEV1")
        );

        var result = factory.create(AtConfiguration.PartyIdType.ENERGY_COMMUNITY, DATE_TIME);

        assertEquals("DEV1EC123456T1788264000000", result);
    }

    @Test
    void constructor_withInvalidPrefix_throws() {
        var configuration = new AtConfiguration("EP123456", null, null, "DEV-");

        var exception = assertThrows(IllegalArgumentException.class, () -> new EdaGroupingIdFactory(configuration));

        assertEquals(
                "region-connector.at.eda.conversation-id.prefix must contain only ASCII letters and digits",
                exception.getMessage()
        );
    }
}
