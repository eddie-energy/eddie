package energy.eddie.regionconnector.us.green.button.data.needs;

import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.dataneeds.rules.DataNeedRule.ValidatedHistoricalDataDataNeedRule;
import org.junit.jupiter.api.Test;

import static energy.eddie.dataneeds.rules.DataNeedRule.AccountingPointDataNeedRule;
import static energy.eddie.regionconnector.us.green.button.data.needs.GreenButtonDataNeedRuleSet.SUPPORTED_GRANULARITIES;
import static org.assertj.core.api.Assertions.assertThat;

class GreenButtonDataNeedRuleSetTest {
    @Test
    void testRuleSet() {
        // Given
        var ruleSet = new GreenButtonDataNeedRuleSet();

        // When
        var res = ruleSet.dataNeedRules();

        // Then
        assertThat(res)
                .containsExactlyInAnyOrder(
                        new AccountingPointDataNeedRule(),
                        new ValidatedHistoricalDataDataNeedRule(EnergyType.ELECTRICITY, SUPPORTED_GRANULARITIES),
                        new ValidatedHistoricalDataDataNeedRule(EnergyType.NATURAL_GAS, SUPPORTED_GRANULARITIES),
                        new ValidatedHistoricalDataDataNeedRule(EnergyType.HYDROGEN, SUPPORTED_GRANULARITIES),
                        new ValidatedHistoricalDataDataNeedRule(EnergyType.HEAT, SUPPORTED_GRANULARITIES)
                );
    }
}