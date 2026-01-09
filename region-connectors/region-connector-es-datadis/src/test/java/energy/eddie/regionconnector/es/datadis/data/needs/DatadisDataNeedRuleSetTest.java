package energy.eddie.regionconnector.es.datadis.data.needs;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.dataneeds.rules.DataNeedRule.AccountingPointDataNeedRule;
import energy.eddie.dataneeds.rules.DataNeedRule.ValidatedHistoricalDataDataNeedRule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DatadisDataNeedRuleSetTest {
    @Test
    void testRuleSet() {
        // Given
        var ruleSet = new DatadisDataNeedRuleSet();

        // When
        var res = ruleSet.dataNeedRules();

        // Then
        assertThat(res)
                .containsExactlyInAnyOrder(
                        new AccountingPointDataNeedRule(),
                        new ValidatedHistoricalDataDataNeedRule(EnergyType.ELECTRICITY,
                                                                List.of(Granularity.PT15M, Granularity.PT1H))
                );
    }
}