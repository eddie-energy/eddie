package energy.eddie.regionconnector.shared.services.data.needs;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.needs.aiida.InboundAiidaDataNeed;
import energy.eddie.dataneeds.supported.DataNeedRule.AccountingPointDataNeedRule;
import energy.eddie.dataneeds.supported.DataNeedRule.AiidaDataNeedRule;
import energy.eddie.dataneeds.supported.DataNeedRule.ValidatedHistoricalDataDataNeedRule;
import energy.eddie.regionconnector.shared.services.data.needs.MatchedRules.Matched;
import energy.eddie.regionconnector.shared.services.data.needs.MatchedRules.MatchedVHDRules;
import energy.eddie.regionconnector.shared.services.data.needs.MatchedRules.NoMatch;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DataNeedRuleMatcherTest {

    @Test
    void givenNoMatchingRule_whenFind_thenReturnNoMatch() {
        // Given
        var matcher = new DataNeedRuleMatcher(
                new AccountingPointDataNeed(),
                () -> List.of(new AiidaDataNeedRule<>(InboundAiidaDataNeed.class))
        );

        // When
        var res = matcher.find();

        // Then
        assertThat(res)
                .isInstanceOf(NoMatch.class);
    }

    @Test
    void givenAccountingPointRuleAndAccountingPointDataNeed_whenFind_thenReturnMatched() {
        // Given
        var matcher = new DataNeedRuleMatcher(
                new AccountingPointDataNeed(),
                () -> List.of(new AccountingPointDataNeedRule())
        );

        // When
        var res = matcher.find();

        // Then
        assertThat(res)
                .isInstanceOf(Matched.class);
    }

    @Test
    void givenValidatedHistoricalDataDataNeedRuleAndValidatedHistoricalDataDataNeed_whenFind_thenReturnMatchedVHDRules() {
        // Given
        var matcher = new DataNeedRuleMatcher(
                new ValidatedHistoricalDataDataNeed(
                        new RelativeDuration(null, null, null),
                        EnergyType.ELECTRICITY,
                        Granularity.PT1H,
                        Granularity.P1D
                ),
                () -> List.of(new ValidatedHistoricalDataDataNeedRule(
                        EnergyType.ELECTRICITY,
                        List.of(Granularity.P1D)
                ))
        );

        // When
        var res = matcher.find();

        // Then
        assertThat(res)
                .isInstanceOf(MatchedVHDRules.class);
    }
}