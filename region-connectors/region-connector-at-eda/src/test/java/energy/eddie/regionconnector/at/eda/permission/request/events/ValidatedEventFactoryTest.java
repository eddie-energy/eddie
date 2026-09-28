// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.permission.request.events;

import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.requests.EdaGroupingIdFactory;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;

class ValidatedEventFactoryTest {
    private static final LocalDate TODAY = LocalDate.of(2026, Month.SEPTEMBER, 1);

    @Test
    void createValidatedEvent_prefixesEligiblePartyConversationId() {
        var event = createValidatedEvent(
                new AtConfiguration("EP123456", null, "DEV")
        );

        assertThat(event.conversationId()).startsWith("DEVEP123456T");
    }

    private static ValidatedEvent createValidatedEvent(AtConfiguration configuration) {
        return new ValidatedEventFactory(new EdaGroupingIdFactory(configuration)).createValidatedEvent(
                "permission-id",
                TODAY,
                TODAY,
                null
        );
    }
}
