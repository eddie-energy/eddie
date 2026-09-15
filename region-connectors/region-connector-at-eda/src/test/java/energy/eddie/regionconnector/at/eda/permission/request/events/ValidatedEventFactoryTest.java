// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.permission.request.events;

import energy.eddie.api.agnostic.data.needs.AccountingPointDataNeedResult;
import energy.eddie.api.agnostic.data.needs.CESUJoinRequestDataNeedResult;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationResult;
import energy.eddie.api.agnostic.data.needs.Timeframe;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.CESUJoinRequestDataNeed;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.requests.EdaGroupingIdFactory;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ValidatedEventFactoryTest {
    private static final LocalDate TODAY = LocalDate.of(2026, Month.SEPTEMBER, 1);

    @Test
    void createValidatedEvent_prefixesEligiblePartyConversationId() {
        var event = createValidatedEvent(
                new AtConfiguration("EP123456", null, null, "DEV"),
                new AccountingPointDataNeedResult(new Timeframe(TODAY, TODAY), new AccountingPointDataNeed())
        );

        assertThat(event.conversationId()).startsWith("DEVEP123456T");
    }

    @Test
    void createValidatedEvent_prefixesEnergyCommunityConversationId() {
        var event = createValidatedEvent(
                new AtConfiguration("EP123456", "community", "EC123456", "DEV"),
                new CESUJoinRequestDataNeedResult(
                        new Timeframe(TODAY, TODAY),
                        List.of(),
                        new CESUJoinRequestDataNeed()
                )
        );

        assertThat(event.conversationId()).startsWith("DEVEC123456T");
    }

    private static ValidatedEvent createValidatedEvent(
            AtConfiguration configuration,
            DataNeedCalculationResult dataNeedCalculationResult
    ) {
        return new ValidatedEventFactory(new EdaGroupingIdFactory(configuration)).createValidatedEvent(
                "permission-id",
                TODAY,
                TODAY,
                null,
                dataNeedCalculationResult
        );
    }
}
